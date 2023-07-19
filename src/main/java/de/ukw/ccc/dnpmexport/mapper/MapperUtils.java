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

import de.itc.onkostar.api.Disease;
import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Procedure;
import de.ukw.ccc.bwhc.dto.Icd10;
import de.ukw.ccc.bwhc.dto.IcdO3T;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
                        .withVersion(getVersion(disease.getIcd10Version()))
                        .withCode(disease.getIcd10Code())
                        .build()
        );
    }

    public List<Integer> getMolekulargenetikProcedureIdsForTherapieplan(Procedure procedure) {
        if (null == procedure || !procedure.getFormName().equals("DNPM Therapieplan")) {
            logger.warn("Ignoring - not of form 'DNPM Therapieplan'!");
            return List.of();
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
                .map(Procedure::getId)
                .collect(Collectors.toList());
    }

    public Stream<Procedure> getTherapieplanRelatedToKlinikAnamnese(Procedure procedure) {
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
                });
    }

}
