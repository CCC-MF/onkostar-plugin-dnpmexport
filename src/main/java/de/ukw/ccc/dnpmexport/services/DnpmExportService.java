/*
 * MIT License
 *
 * Copyright (c) 2024 Comprehensive Cancer Center Mainfranken
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.ukw.ccc.dnpmexport.services;

import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Procedure;
import de.ukw.ccc.bwhc.dto.*;
import de.ukw.ccc.dnpmexport.ExportException;
import de.ukw.ccc.dnpmexport.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DnpmExportService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final IOnkostarApi onkostarApi;

    private final MapperUtils mapperUtils;

    private final RestTemplate restTemplate;

    public DnpmExportService(final IOnkostarApi onkostarApi, final RestTemplate restTemplate) {
        this.onkostarApi = onkostarApi;
        this.mapperUtils = new MapperUtils(onkostarApi);
        this.restTemplate = restTemplate;
    }

    public void export(Procedure procedure) throws ExportException {
        if (procedure.getFormName().equals("DNPM Klinik/Anamnese")) {
            if (shouldExportMtbFile(procedure).orElse(false)) {
                exportKlinikAnamneseRelatedData(procedure).ifPresent(this::sendMtbFileRequest);
            } else {
                sendDeleteRequest(procedure.getPatient().getPatientId());
            }
        } else if (procedure.getFormName().equals("DNPM Therapieplan")) {
            findRelatedKlinikAnamnese(procedure)
                    .ifPresent(klinikAnamnese -> {
                        if (shouldExportMtbFile(klinikAnamnese).orElse(false)) {
                            exportKlinikAnamneseRelatedData(klinikAnamnese).ifPresent(this::sendMtbFileRequest);
                        } else {
                            sendDeleteRequest(procedure.getPatient().getPatientId());
                        }
                    });
        } else if (procedure.getFormName().equals("DNPM FollowUp")) {
            findRelatedEinzelempfehlung(procedure)
                    .flatMap(this::findParentTherapieplan)
                    .flatMap(this::findRelatedKlinikAnamnese)
                    .ifPresent(klinikAnamnese -> {
                        if (shouldExportMtbFile(klinikAnamnese).orElse(false)) {
                            exportKlinikAnamneseRelatedData(klinikAnamnese).ifPresent(this::sendMtbFileRequest);
                        } else {
                            sendDeleteRequest(procedure.getPatient().getPatientId());
                        }
                    });
        }
    }

    private Optional<Procedure> findRelatedEinzelempfehlung(Procedure procedure) {
        return mapperUtils.findEinzelempfehlungRelatedToFollowUp(procedure);
    }

    private Optional<Procedure> findParentTherapieplan(Procedure procedure) {
        return mapperUtils.findTherapieplanRelatedToEinzelempfehlung(procedure);
    }

    private Optional<Procedure> findRelatedKlinikAnamnese(Procedure procedure) {
        return mapperUtils.findKlinikAnamneseRelatedToTherapieplan(procedure);
    }

    private void sendMtbFileRequest(MtbFile mtbFile) throws ExportException {
        var exportUrl = onkostarApi.getGlobalSetting("dnpmexport_url");

        try {
            var uri = URI.create(exportUrl);
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (uri.getUserInfo() != null) {
                headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(uri.getUserInfo().getBytes()));
            }

            var entityReq = new HttpEntity<>(mtbFile, headers);

            var r = restTemplate.postForEntity(uri, entityReq, String.class);
            if (!r.getStatusCode().is2xxSuccessful()) {
                logger.warn("Error sending to remote system: {}", r.getBody());
                throw new ExportException("Kann Daten nicht an das externe System senden");
            }
        } catch (IllegalArgumentException e) {
            logger.error("Not a valid URI to export to: '{}'", exportUrl);
            throw new ExportException("Keine gültige Adresse für das externe System");
        } catch (RestClientException e) {
            logger.error("Cannot send data to remote system", e);
            throw new ExportException("Kann Daten nicht an das externe System senden");
        }
    }

    private void sendDeleteRequest(String patientId) throws ExportException {
        var exportUrl = onkostarApi.getGlobalSetting("dnpmexport_url");

        try {
            var uri = URI.create(exportUrl + "/" + patientId);
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (uri.getUserInfo() != null) {
                headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(uri.getUserInfo().getBytes()));
            }

            var entityReq = new HttpEntity<>(null, headers);

            restTemplate.exchange(uri, HttpMethod.DELETE, entityReq, String.class);
        } catch (IllegalArgumentException e) {
            logger.error("Not a valid URI to export to: '{}'", exportUrl);
            throw new ExportException("Keine gültige Adresse für das externe System");
        } catch (RestClientException e) {
            logger.error("Cannot send data to remote system", e);
            throw new ExportException("Kann Daten nicht an das externe System senden");
        }
    }

    private Optional<Boolean> shouldExportMtbFile(Procedure procedure) {
        if (null == procedure || !procedure.getFormName().equals("DNPM Klinik/Anamnese")) {
            logger.warn("Ignoring - not of form 'DNPM Klinik/Anamnese'!");
            return Optional.empty();
        }

        var consent = new KlinikAnamneseToConsentMapper(mapperUtils).apply(procedure);

        var exportWithConsentRejected = null != onkostarApi.getGlobalSetting("dnpmexport_export_consent_rejected")
                && onkostarApi.getGlobalSetting("dnpmexport_export_consent_rejected").equals("true");

        return Optional.of(
                exportWithConsentRejected || (consent.isPresent() && consent.get().getStatus() == Consent.Status.ACTIVE)
        );
    }

    private Optional<MtbFile> exportKlinikAnamneseRelatedData(Procedure procedure) {
        if (null == procedure || !procedure.getFormName().equals("DNPM Klinik/Anamnese")) {
            logger.warn("Ignoring - not of form 'DNPM Klinik/Anamnese'!");
            return Optional.empty();
        }

        var patient = new PatientMapper().apply(procedure.getPatient());
        var consent = new KlinikAnamneseToConsentMapper(mapperUtils).apply(procedure);
        var episode = new KlinikAnamneseToEpisodeMapper(mapperUtils).apply(procedure);
        var diagnose = new KlinikAnamneseToDiagnoseMapper(mapperUtils).apply(procedure);

        var exportWithConsentRejected = null != onkostarApi.getGlobalSetting("dnpmexport_export_consent_rejected")
                && onkostarApi.getGlobalSetting("dnpmexport_export_consent_rejected").equals("true");

        var mtbFile = MtbFile.builder();
        if (!exportWithConsentRejected && (consent.isEmpty() || consent.get().getStatus() != Consent.Status.ACTIVE)) {
            logger.warn("Ignoring - No consent!");
            return Optional.empty();
        }
        consent.ifPresent(mtbFile::withConsent);

        /* Patient **/
        if (patient.isEmpty()) {
            logger.error("No patient found");
            return Optional.empty();
        }
        mtbFile.withPatient(patient.get());

        /* Episode **/
        if (episode.isEmpty()) {
            logger.error("No episode info found");
            return Optional.empty();
        }
        mtbFile.withEpisode(episode.get());

        /* Diagnose **/
        if (diagnose.isEmpty()) {
            logger.error("No diagnose info found");
            return Optional.empty();
        }

        var result = mtbFile.build();

        // Maps from diagnosis form
        //result.getDiagnoses().addAll(getDiagnoses(procedure));

        // Maps from Klinik/Anamnese form
        diagnose.ifPresent(diagnosis -> result.getDiagnoses().add(diagnosis));

        result.getCarePlans().addAll(getCarePlans(procedure));
        result.getFamilyMemberDiagnoses().addAll(getFamilyMemberDiagnoses(procedure));
        result.getEcogStatus().addAll(getEcogStatusList(procedure));
        result.getRebiopsyRequests().addAll(getRebiopsyRequests(procedure));
        result.getRecommendations().addAll(getRecommendations(procedure));
        result.getSpecimens().addAll(getSpecimens(procedure));
        result.getStudyInclusionRequests().addAll(getStudyInclusionRequests(procedure));
        result.getHistologyReevaluationRequests().addAll(getHistologyReevaluationRequests(procedure));
        result.getGeneticCounsellingRequests().addAll(getGeneticCounsellingRequests(procedure));

        /* MolGen */

        result.getNgsReports().addAll(getNgsReports(procedure));

        /* FollowUp */

        result.getClaims().addAll(getClaims(procedure));
        result.getClaimResponses().addAll(getClaimResponses(procedure));
        result.getMolecularTherapies().addAll(getMolecularTherapies(procedure));
        result.getResponses().addAll(getResponses(procedure));

        return Optional.of(result);
    }

    private List<Diagnosis> getDiagnoses(Procedure procedure) {
        return procedure.getDiseases().stream()
                .map(d -> new DiseaseToDiagnoseMapper(mapperUtils).apply(d))
                .map(d -> d.orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<CarePlan> getCarePlans(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .map(
                        p -> new TherapieplanToCarePlanMapper(mapperUtils).apply(p)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<FamilyMemberDiagnosis> getFamilyMemberDiagnoses(Procedure procedure) {
        return new KlinikAnamneseToFamilyMemberDiagnosisMapper(mapperUtils).apply(procedure);
    }

    private List<Ecogstatus> getEcogStatusList(Procedure procedure) {
        return new KlinikAnamneseToEcogStatusMapper(mapperUtils).apply(procedure);
    }

    private List<RebiopsyRequest> getRebiopsyRequests(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        p -> new TherapieplanToRebiopsyRequestMapper(mapperUtils).apply(p).stream()
                )
                .collect(Collectors.toList());
    }

    private List<Recommendation> getRecommendations(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        p -> new TherapieplanToRecommendationMapper(mapperUtils).apply(p).stream()
                )
                .collect(Collectors.toList());
    }

    private List<Specimens> getSpecimens(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        p -> new MapperUtils(onkostarApi).getMolekulargenetikProcedureIdsForTherapieplan(p)
                )
                .distinct()
                .map(onkostarApi::getProcedure)
                .filter(Objects::nonNull)
                .map(p -> new MolekulargenetikToSpecimenMapper(mapperUtils).apply(p))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<StudyInclusionRequest> getStudyInclusionRequests(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        p -> new TherapieplanToStudyInclusionMapper(mapperUtils).apply(p).stream()
                )
                .collect(Collectors.toList());
    }

    private List<HistologyReevaluationRequest> getHistologyReevaluationRequests(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        p -> new TherapieplanToHistologyReevaluationRequestMapper(mapperUtils).apply(p).stream()
                )
                .collect(Collectors.toList());
    }

    private List<GeneticCounsellingRequest> getGeneticCounsellingRequests(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        p -> new TherapieplanToGeneticCounsellingRequestMapper(mapperUtils).apply(p).stream()
                )
                .collect(Collectors.toList());
    }

    private List<NgsReport> getNgsReports(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        mapperUtils::getMolekulargenetikProcedureIdsForTherapieplan
                )
                .map(onkostarApi::getProcedure)
                .filter(Objects::nonNull)
                .map(
                        p -> new MolekulargenetikToNgsReportMapper(mapperUtils).apply(p)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<Claim> getClaims(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        mapperUtils::getEinzelempfehlungRelatedToTherapieplan
                )
                .flatMap(
                        mapperUtils::getFollowUpsRelatedToEinzelempfehlung
                )
                .map(
                        p -> new FollowUpToClaimMapper(mapperUtils).apply(p)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<ClaimResponse> getClaimResponses(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        mapperUtils::getEinzelempfehlungRelatedToTherapieplan
                )
                .flatMap(
                        mapperUtils::getFollowUpsRelatedToEinzelempfehlung
                )
                .map(
                        p -> new FollowUpToClaimResponseMapper(mapperUtils).apply(p)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<MolecularTherapy> getMolecularTherapies(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        mapperUtils::getEinzelempfehlungRelatedToTherapieplan
                )
                .map(
                        mapperUtils::getFollowUpsRelatedToEinzelempfehlung
                )
                .map(
                        followUp -> new MolecularTherapy(
                                followUp.map(p -> new FollowUpToHistoryMapper(mapperUtils).apply(p)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())
                        )
                )
                .collect(Collectors.toList());
    }

    private List<Response> getResponses(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        mapperUtils::getEinzelempfehlungRelatedToTherapieplan
                )
                .flatMap(
                        mapperUtils::getFollowUpsRelatedToEinzelempfehlung
                )
                .map(
                        followUp -> new FollowUpToResponseMapper(mapperUtils).apply(followUp)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

}
