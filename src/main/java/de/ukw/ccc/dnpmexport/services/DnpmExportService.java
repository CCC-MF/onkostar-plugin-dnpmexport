/*
 * MIT License
 *
 * Copyright (c) 2023 Comprehensive Cancer Center Mainfranken
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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Procedure;
import de.ukw.ccc.bwhc.dto.*;
import de.ukw.ccc.dnpmexport.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DnpmExportService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final IOnkostarApi onkostarApi;

    private final MapperUtils mapperUtils;

    public DnpmExportService(final IOnkostarApi onkostarApi) {
        this.onkostarApi = onkostarApi;
        this.mapperUtils = new MapperUtils(onkostarApi);
    }

    public void export(Procedure procedure) {
        if (procedure.getFormName().equals("DNPM Klinik/Anamnese")) {
            exportKlinikAnamneseRelatedData(procedure).ifPresent(mtbFile -> {
                try {
                    var file = new PrintWriter("/testexport.json", StandardCharsets.UTF_8);
                    new ObjectMapper().writeValue(file, mtbFile);
                } catch (Exception e) {
                    logger.error("Error!", e);
                }
            });
        } else if (procedure.getFormName().equals("DNPM Therapieplan")) {
            var procedureId = procedure.getValue("refdnpmklinikanamnese").getInt();
            if (procedureId > 0) {
                exportKlinikAnamneseRelatedData(onkostarApi.getProcedure(procedureId)).ifPresent(mtbFile -> {
                    try {
                        var file = new PrintWriter("/testexport.json", StandardCharsets.UTF_8);
                        new ObjectMapper().writeValue(file, mtbFile);
                    } catch (Exception e) {
                        logger.error("Error!", e);
                    }
                });
            }
        }
    }

    private Optional<MtbFile> exportKlinikAnamneseRelatedData(Procedure procedure) {
        if (null == procedure || !procedure.getFormName().equals("DNPM Klinik/Anamnese")) {
            logger.warn("Ignoring - not of form 'DNPM Klinik/Anamnese'!");
            return Optional.empty();
        }

        if (procedure.getDiseases().size() != 1) {
            logger.warn("Ignoring - more than one disease!");
            return Optional.empty();
        }

        var patient = new PatientMapper().apply(procedure.getPatient());
        var consent = new KlinikAnamneseToConsentMapper().apply(procedure);
        var episode = new KlinikAnamneseToEpisodeMapper().apply(procedure);
        var diagnose = new DiseaseToDiagnoseMapper(onkostarApi).apply(procedure.getDiseases().get(0));

        var mtbFile = MtbFile.builder();
        /* Patient **/
        if (patient.isEmpty()) {
            logger.error("No patient found");
            return Optional.empty();
        }
        mtbFile.withPatient(patient.get());

        /* Consent **/
        if (consent.isEmpty()) {
            logger.error("No consent info found");
            return Optional.empty();
        }
        mtbFile.withConsent(consent.get());

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

        result.getDiagnoses().addAll(getDiagnoses(procedure));
        result.getCarePlans().addAll(getCarePlans(procedure));
        result.getFamilyMemberDiagnoses().addAll(getFamilyMemberDiagnoses(procedure));
        result.getEcogStatus().addAll(getEcogStatusList(procedure));
        result.getRebiopsyRequests().addAll(getRebiopsyRequests(procedure));
        result.getRecommendations().addAll(getRecommendations(procedure));
        result.getSpecimens().addAll(getSpecimens(procedure));
        result.getStudyInclusionRequests().addAll(getStudyInclusionRequests(procedure));
        result.getHistologyReevaluationRequests().addAll(getHistologyReevaluationRequests(procedure));

        return Optional.of(result);
    }

    private List<Diagnosis> getDiagnoses(Procedure procedure) {
        return procedure.getDiseases().stream()
                .map(d -> new DiseaseToDiagnoseMapper(onkostarApi).apply(d))
                .map(d -> d.orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<CarePlan> getCarePlans(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .map(
                        p -> new TherapieplanToCarePlanMapper(onkostarApi).apply(p)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<FamilyMemberDiagnosis> getFamilyMemberDiagnoses(Procedure procedure) {
        return new KlinikAnamneseToFamilyMemberDiagnosisMapper(onkostarApi).apply(procedure);
    }

    private List<Ecogstatus> getEcogStatusList(Procedure procedure) {
        return new KlinikAnamneseToEcogStatusMapper(onkostarApi).apply(procedure);
    }

    private List<RebiopsyRequest> getRebiopsyRequests(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        p -> new TherapieplanToRebiopsyRequestMapper(onkostarApi).apply(p).stream()
                )
                .collect(Collectors.toList());
    }

    private List<Recommendation> getRecommendations(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        p -> new TherapieplanToRecommendationMapper(onkostarApi).apply(p).stream()
                )
                .collect(Collectors.toList());
    }

    private List<Specimens> getSpecimens(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        p -> new MapperUtils(onkostarApi).getMolekulargenetikProcedureIdsForTherapieplan(p).stream()
                )
                .distinct()
                .map(onkostarApi::getProcedure)
                .filter(Objects::nonNull)
                .map(p -> new MolekulargenetikToSpecimenMapper(onkostarApi).apply(p))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<StudyInclusionRequest> getStudyInclusionRequests(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        p -> new TherapieplanToStudyInclusionMapper(onkostarApi).apply(p).stream()
                )
                .collect(Collectors.toList());
    }

    private List<HistologyReevaluationRequest> getHistologyReevaluationRequests(Procedure procedure) {
        return mapperUtils.getTherapieplanRelatedToKlinikAnamnese(procedure)
                .flatMap(
                        p -> new TherapieplanToHistologyReevaluationRequestMapper(onkostarApi).apply(p).stream()
                )
                .collect(Collectors.toList());
    }

}
