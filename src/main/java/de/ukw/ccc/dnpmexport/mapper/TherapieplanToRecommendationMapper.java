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

import de.itc.onkostar.api.Procedure;
import de.ukw.ccc.bwhc.dto.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TherapieplanToRecommendationMapper extends TherapieplanMapper<List<Recommendation>> {

    public TherapieplanToRecommendationMapper(final MapperUtils mapperUtils) {
        super(mapperUtils);
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
                    var molgenref = procedure.getValue("refosmolekulargenetik");
                    var builder = Recommendation.builder()
                            .withId(anonymizeId(p))
                            .withPatient(getPatientId(procedure))
                            .withLevelOfEvidence(levelOfEvidence(p))
                            .withPriority(priority(p))
                            //.withSupportingVariants() // TODO: Einfügen, wenn OS.Molekulargenetik fertig
                            ;

                    var issuedOn = mapperUtils.einzelempfehlungMtbDate(p);
                    if (issuedOn != null && !issuedOn.isEmpty()) {
                            builder.withIssuedOn(issuedOn);
                    }

                    // Aktuell nur eine einzige Referenz, später ggf mehrere in Datenmodell V2
                    mapperUtils.getMolekulargenetikProcedureIdsForEinzelempfehlung(p)
                            .forEach(molgen -> builder.withNgsReport(molgen.toString()));

                    mapperUtils.findKlinikAnamneseRelatedToTherapieplan(procedure)
                            .ifPresent(klinikAnamnese -> builder.withDiagnosis(mapperUtils.anonymizeId(klinikAnamnese.getId().toString())));

                    if (null != molgenref) {
                        builder.withNgsReport(anonymizeString(molgenref.getString()));
                    }

                    var recommendation = builder.build();
                    var m = medications(p);
                    recommendation.getMedication().addAll(m);

                    return recommendation;
                })
                .collect(Collectors.toList());
    }

    private Recommendation.Priority priority(Procedure procedure) {
        final var value = procedure.getValue("prio");
        if (null == value) {
            return null;
        }

        switch (value.getString()) {
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

        final var value = procedure.getValue("evidenzlevel");
        if (null == value) {
            return null;
        }

        switch (value.getString()) {
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
        final var value = procedure.getValue("evidenzlevelzusatz");
        if (null == value) {
            return Set.of();
        }
        switch (value.getString()) {
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
        final var medications = new JsonToMedicationMapper().apply(procedure.getValue("wirkstoffejson").getString());
        return medications.orElseGet(List::of);
    }

}
