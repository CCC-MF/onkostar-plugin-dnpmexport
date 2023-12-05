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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.itc.onkostar.api.Procedure;
import de.ukw.ccc.bwhc.dto.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TherapieplanToRecommendationMapper extends TherapieplanMapper<List<Recommendation>> {

    private final ObjectMapper objectMapper;

    public TherapieplanToRecommendationMapper(final MapperUtils mapperUtils) {
        super(mapperUtils);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Recommendation> apply(Procedure procedure) {
        if (!canApply(procedure)) {
            return List.of();
        }

        // Only one diagnosis!
        if (procedure.getDiseases().size() != 1) {
            return List.of();
        }

        return mapperUtils.onkostarApi()
                .getProceduresForDiseaseByForm(procedure.getDiseaseIds().get(0), "DNPM UF Einzelempfehlung")
                .stream()
                .filter(p -> p.getParentProcedureId() == procedure.getId())
                .map(p -> {
                    var builder = Recommendation.builder()
                            .withId(anonymizeId(p))
                            .withPatient(getPatientId(procedure))
                            .withDiagnosis(mapperUtils.anonymizeId(procedure.getDiseaseIds().get(0).toString()))
                            .withIssuedOn(issuedOn(p))
                            .withLevelOfEvidence(levelOfEvidence(p))
                            .withPriority(priority(p))
                            .withNgsReport(anonymizeString(procedure.getValue("refosmolekulargenetik").getString()))
                            //.withSupportingVariants() // TODO: Einfügen, wenn OS.Molekulargenetik fertig
                            ;
                    var recommendation = builder.build();
                    recommendation.getMedication().addAll(medications(p));

                    return recommendation;
                })
                .collect(Collectors.toList());
    }

    private String issuedOn(Procedure procedure) {
        return mapperUtils.einzelempfehlungMtbDate(procedure);
    }

    private Recommendation.Priority priority(Procedure procedure) {
        switch (procedure.getValue("prio").getString()) {
            case "1":
                return Recommendation.Priority._1;
            case "2":
                return Recommendation.Priority._2;
            case "3":
                return Recommendation.Priority._3;
            default:
                return Recommendation.Priority._4;
        }
    }

    private LevelOfEvidence levelOfEvidence(Procedure procedure) {
        var addendums = addendums(procedure);

        switch (procedure.getValue("evidenzlevel").getString()) {
            case "1":
                return LevelOfEvidence.builder()
                        .withGrading(Grading.builder().withCode(Grading.GradingCode.M_1_A).build())
                        .withAddendums(addendums)
                        .build();
            case "2":
                return LevelOfEvidence.builder()
                        .withGrading(Grading.builder().withCode(Grading.GradingCode.M_1_B).build())
                        .withAddendums(addendums)
                        .build();
            case "3":
                return LevelOfEvidence.builder()
                        .withGrading(Grading.builder().withCode(Grading.GradingCode.M_1_C).build())
                        .withAddendums(addendums)
                        .build();
            case "4":
                return LevelOfEvidence.builder()
                        .withGrading(Grading.builder().withCode(Grading.GradingCode.M_2_A).build())
                        .withAddendums(addendums)
                        .build();
            case "5":
                return LevelOfEvidence.builder()
                        .withGrading(Grading.builder().withCode(Grading.GradingCode.M_2_B).build())
                        .withAddendums(addendums)
                        .build();
            case "6":
                return LevelOfEvidence.builder()
                        .withGrading(Grading.builder().withCode(Grading.GradingCode.M_2_C).build())
                        .withAddendums(addendums)
                        .build();
            case "7":
                return LevelOfEvidence.builder()
                        .withGrading(Grading.builder().withCode(Grading.GradingCode.M_3).build())
                        .withAddendums(addendums)
                        .build();
            case "8":
                return LevelOfEvidence.builder()
                        .withGrading(Grading.builder().withCode(Grading.GradingCode.M_4).build())
                        .withAddendums(addendums)
                        .build();
            default:
                return null;
        }
    }

    private Set<Addendum> addendums(Procedure procedure) {
        switch (procedure.getValue("evidenzlevelzusatz").getString()) {
            case "s":
                return Set.of(Addendum.builder().withCode("is").build());
            case "v":
                return Set.of(Addendum.builder().withCode("iv").build());
            case "z":
                return Set.of(Addendum.builder().withCode("Z").build());
            case "r":
                return Set.of(Addendum.builder().withCode("R").build());
            default:
                return Set.of();
        }
    }

    private List<Medication> medications(Procedure procedure) {
        try {
            var wirkstoffejson = procedure.getValue("wirkstoffejson").getString();
            return objectMapper.readValue(wirkstoffejson, new TypeReference<List<Wirkstoff>>() {
                    }).stream()
                    .map(wirkstoff -> Medication.builder()
                            .withCode(wirkstoff.code)
                            .withSystem(
                                    // Wirkstoff ohne Version => UNREGISTERED
                                    "ATC".equals(wirkstoff.system) && null != wirkstoff.version && !wirkstoff.version.isBlank()
                                            ? Medication.System.ATC
                                            : Medication.System.UNREGISTERED
                            )
                            .withVersion(wirkstoff.version)
                            .withDisplay(wirkstoff.name)
                            .build()
                    )
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private static class Wirkstoff {
        private String code;
        private String name;
        private String system;
        private String version;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSystem() {
            return system;
        }

        public void setSystem(String system) {
            this.system = system;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

    }

}
