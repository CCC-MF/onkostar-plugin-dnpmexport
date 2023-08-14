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

import java.util.Optional;
import java.util.stream.Collectors;

public class TherapieplanToCarePlanMapper extends TherapieplanMapper<Optional<CarePlan>> {

    public TherapieplanToCarePlanMapper(final MapperUtils mapperUtils) {
        super(mapperUtils);
    }

    @Override
    public Optional<CarePlan> apply(Procedure procedure) {
        if (null == procedure || !procedure.getFormName().equals("DNPM Therapieplan")) {
            return Optional.empty();
        }

        // Only one diagnosis!
        if (procedure.getDiseases().size() != 1) {
            return Optional.empty();
        }

        var protokollauszug = procedure.getValue("protokollauszug");

        var carePlanBuilder = CarePlan.builder()
                .withId(anonymizeId(procedure))
                .withIssuedOn(dateFormat().format(procedure.getStartDate()))
                .withPatient(getPatientId(procedure))
                .withDescription(protokollauszug == null ? "" : protokollauszug.getString())
                .withDiagnosis(mapperUtils.anonymizeId(procedure.getDiseaseIds().get(0).toString()));

        var targetFinding = procedure.getValue("target").getString();
        if (targetFinding.equals("KT")) {
            carePlanBuilder.withNoTargetFinding(
                    NoTargetFinding.builder()
                            .withDiagnosis(mapperUtils.anonymizeId(procedure.getDiseaseIds().get(0).toString()))
                            .withPatient(getPatientId(procedure))
                            .withIssuedOn(dateFormat().format(procedure.getStartDate()))
                            .build()
            );
        }

        var humangenBeratung = procedure.getValue("humangenberatung").getString();
        if (humangenBeratung.equals("1")) {
            carePlanBuilder.withGeneticCounsellingRequest(mapperUtils.anonymizeId(procedure.getId().toString()));
        }

        var carePlan = carePlanBuilder.build();

        var rebiopsie = procedure.getValue("mitempfehlungrebiopsie").getBoolean();
        if (rebiopsie) {
            carePlan.getRebiopsyRequests().addAll(
                    new TherapieplanToRebiopsyRequestMapper(mapperUtils).apply(procedure).stream()
                            .map(RebiopsyRequest::getId)
                            .collect(Collectors.toList())
            );
        }

        var einzelempfehlung = procedure.getValue("miteinzelempfehlung").getBoolean();
        if (einzelempfehlung) {
            carePlan.getRecommendations().addAll(
                    new TherapieplanToRecommendationMapper(mapperUtils).apply(procedure).stream()
                            .map(Recommendation::getId)
                            .collect(Collectors.toList())
            );
        }

        carePlan.getStudyInclusionRequests().addAll(
                new TherapieplanToStudyInclusionMapper(mapperUtils).apply(procedure).stream()
                        .map(StudyInclusionRequest::getId)
                        .collect(Collectors.toList())
        );

        return Optional.of(carePlan);
    }

}
