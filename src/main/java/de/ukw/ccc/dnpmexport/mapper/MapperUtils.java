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

package de.ukw.ccc.dnpmexport.mapper;

import de.itc.onkostar.api.*;
import de.ukw.ccc.bwhc.dto.Icd10;
import de.ukw.ccc.bwhc.dto.IcdO3T;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapperUtils {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final IOnkostarApi onkostarApi;

    public MapperUtils(final IOnkostarApi onkostarApi) {
        this.onkostarApi = onkostarApi;
    }

    public IOnkostarApi onkostarApi() {
        return this.onkostarApi;
    }

    // TODO Seek a way to get ICD PropertyCatalogue Version Description which contains year in the last 4 digits
    public String getVersion(int versionId) {
        var oid = onkostarApi.getPropertyCatalogueVersionOid(versionId);
        if (null == oid) {
            return "";
        }

        return oid.substring(oid.length() - 4);
    }

    public Optional<Icd10> getIcd10(Disease disease) {
        if (null == disease.getIcd10Code() || disease.getIcd10Code().isBlank()) {
            return Optional.empty();
        }

        if (null == disease.getIcd10Version() || getVersion(disease.getIcd10Version()).isBlank()) {
            return Optional.empty();
        }

        return Optional.of(
                Icd10.builder()
                        .withVersion(getVersion(disease.getIcd10Version()))
                        .withCode(disease.getIcd10Code())
                        .build()
        );
    }

    public Optional<IcdO3T> getIcdO3T(Disease disease) {
        if (null == disease.getLocalisationCode() || disease.getLocalisationCode().isBlank()) {
            return Optional.empty();
        }

        if (null == disease.getLocalisationVersion() || getVersion(disease.getLocalisationVersion()).isBlank()) {
            return Optional.empty();
        }

        return Optional.of(
                IcdO3T.builder()
                        .withVersion(getVersion(disease.getLocalisationVersion()))
                        .withCode(disease.getLocalisationCode())
                        .build()
        );
    }

    /**
     * Stream of procedures for 'OS.Molekulargenetik' related to given procedure for 'DNPM Therapieplan'
     *
     * @param procedure  A procedure for 'DNPM Therapieplan'
     * @param lockedOnly include locked procedures only
     * @return Stream of procedures
     */
    public Stream<Integer> getMolekulargenetikProcedureIdsForTherapieplan(Procedure procedure, boolean lockedOnly) {
        if (null == procedure || !procedure.getFormName().equals("DNPM Therapieplan")) {
            logger.warn("Ignoring - not of form 'DNPM Therapieplan'!");
            return Stream.empty();
        }

        var refIds = onkostarApi
                .getProceduresForDiseaseByForm(procedure.getDiseaseIds().get(0), "DNPM UF Einzelempfehlung")
                .stream()
                .filter(p -> p.getParentProcedureId() == procedure.getId())
                .map(p -> p.getValue("refosmolekulargenetik").getInt())
                .collect(Collectors.toList());

        refIds.add(procedure.getValue("refreevaltumorprobe").getInt());

        return refIds.stream()
                .distinct()
                .map(onkostarApi::getProcedure)
                .filter(Objects::nonNull)
                .filter(p -> "OS.Molekulargenetik".equals(p.getFormName()))
                .filter(p -> !lockedOnly || p.getEditState() == ProcedureEditStateType.COMPLETED)
                .map(Procedure::getId);
    }

    /**
     * Stream of locked procedures for 'OS.Molekulargenetik' related to given procedure for 'DNPM Therapieplan'
     *
     * @param procedure A procedure for 'DNPM Therapieplan'
     * @return Stream of locked procedures
     */
    public Stream<Integer> getMolekulargenetikProcedureIdsForTherapieplan(Procedure procedure) {
        return getMolekulargenetikProcedureIdsForTherapieplan(procedure, true);
    }

    /**
     * Stream of procedures for 'DNPM Therapieplan' related to given procedure for 'DNPM Klinik/Anamnese'
     *
     * @param procedure  A procedure for 'DNPM Klink/Anamnese'
     * @param lockedOnly include locked procedures only
     * @return Stream of procedures
     */
    public Stream<Procedure> getTherapieplanRelatedToKlinikAnamnese(Procedure procedure, boolean lockedOnly) {
        if (!"DNPM Klinik/Anamnese".equals(procedure.getFormName())) {
            logger.warn("Ignoring - not of form 'DNPM Klinik/Anamnese'!");
            return Stream.empty();
        }
        if (procedure.getDiseases().size() != 1) {
            logger.warn("Ignoring - more than one disease!");
            return Stream.empty();
        }
        return onkostarApi.getProceduresForDiseaseByForm(procedure.getDiseaseIds().get(0), "DNPM Therapieplan").stream()
                .filter(p -> {
                    var refId = p.getValue("refdnpmklinikanamnese").getInt();
                    return procedure.getId().equals(refId);
                })
                .filter(p -> !lockedOnly || p.getEditState() == ProcedureEditStateType.COMPLETED);
    }

    /**
     * Stream of locked procedures for 'DNPM Therapieplan' related to given procedure for 'DNPM Klinik/Anamnese'
     *
     * @param procedure A procedure for 'DNPM Klink/Anamnese'
     * @return Stream of locked procedures
     */
    public Stream<Procedure> getTherapieplanRelatedToKlinikAnamnese(Procedure procedure) {
        return getTherapieplanRelatedToKlinikAnamnese(procedure, true);
    }

    public String einzelempfehlungMtbDate(Procedure procedure) {
        if (!"DNPM UF Einzelempfehlung".equals(procedure.getFormName())) {
            logger.warn("Ignoring - not of form 'DNPM UF Einzelempfehlung'!");
            return "";
        }
        var date = procedure.getValue("ufeedatum").getString();
        if (!date.isBlank()) {
            return date;
        }

        logger.warn("Kein MTB-Datum in 'DNPM UF Einzelempfehlung'!");
        var mtb = onkostarApi.getProcedure(procedure.getValue("mtb").getInt());
        if (null == mtb) {
            logger.warn("Kein MTB in 'DNPM UF Einzelempfehlung'!");
            return "";
        }
        switch (mtb.getFormName()) {
            case "OS.Tumorkonferenz":
            case "OS.Tumorkonferenz.VarianteUKW":
                return mtb.getValue("Datum").getString();
            default:
                return mtb.getValue("Datum").getString();
        }
    }

    /**
     * Creates SHA256 hash and returns Prefix with first 40 digits of base32 encoded hash
     *
     * @param id The procedure ID to be anonymized
     * @return Prefix with first 40 digits of base32 encoded hash
     */
    public String anonymizeId(String id) {
        var base32 = new Base32();
        var prefix = this.onkostarApi.getGlobalSetting("dnpmexport_prefix");
        if (null != prefix) {
            return String.format("%s_%s", prefix, base32.encodeToString(DigestUtils.sha256(id)).substring(0, 41).toLowerCase());
        }
        return String.format("UNKNOWN_%s", base32.encodeToString(DigestUtils.sha256(id)).substring(0, 41).toLowerCase());
    }

    public static String getPatientId(Patient patient) {
        // "SAP-ID" for now
        return patient.getPatientId();
    }

    public static String getPatientId(Procedure procedure) {
        return getPatientId(procedure.getPatient());
    }

    public static String getPatientId(Disease disease) {
        return getPatientId(disease.getPatient());
    }

    public static boolean isPresent(final Procedure procedure, final String fieldName) {
        return null != procedure.getValue(fieldName);
    }

    public static <T> Optional<T> getRequiredValue(final Procedure procedure, final String fieldName) {
        var value = procedure.getValue(fieldName);

        if (null == value) {
            return Optional.empty();
        }


        return Optional.of(value.getValue());
    }

}
