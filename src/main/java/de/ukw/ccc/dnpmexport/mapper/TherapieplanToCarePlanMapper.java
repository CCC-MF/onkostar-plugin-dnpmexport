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
import de.ukw.ccc.bwhc.dto.CarePlan;
import de.ukw.ccc.bwhc.dto.NoTargetFinding;

import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.function.Function;

public class TherapieplanToCarePlanMapper implements Function<Procedure, Optional<CarePlan>> {

    @Override
    public Optional<CarePlan> apply(Procedure procedure) {
        if (null == procedure || !procedure.getFormName().equals("DNPM Therapieplan")) {
            return Optional.empty();
        }

        // Only one diagnosis!
        if (procedure.getDiseases().size() != 1) {
            return Optional.empty();
        }

        var formatter = new SimpleDateFormat("yyyy-MM-dd");

        var protokollauszug = procedure.getValue("protokollauszug");

        var carePlanBuilder = CarePlan.builder()
                .withIssuedOn(formatter.format(procedure.getStartDate()))
                .withId(procedure.getId().toString())
                .withPatient(procedure.getPatient().getId().toString())
                .withDescription(protokollauszug == null ? "" : protokollauszug.getString())
                .withDiagnosis(procedure.getDiseaseIds().get(0).toString());

        var targetFinding = procedure.getValue("target").getString();
        if (targetFinding.equals("KT")) {
            carePlanBuilder.withNoTargetFinding(
                    NoTargetFinding.builder()
                            .withDiagnosis(procedure.getDiseaseIds().get(0).toString())
                            .withPatient(procedure.getPatient().getId().toString())
                            .withIssuedOn(formatter.format(procedure.getStartDate()))
                            .build()
            );
        }

        var humangenBeratung = procedure.getValue("humangenberatung").getString();
        if (humangenBeratung.equals("1")) {
            carePlanBuilder.withGeneticCounsellingRequest(procedure.getId().toString());
        }

        var rebiopsie = procedure.getValue("mitempfehlungrebiopsie").getBoolean();
        if (rebiopsie) {
            //carePlanBuilder.withRebiopsyRequests(procedure.getValue());
        }

        var einzelempfehlung = procedure.getValue("miteinzelempfehlung").getBoolean();
        if (einzelempfehlung) {
            //carePlanBuilder.withRebiopsyRequests(procedure.getValue());
        }

        return Optional.of(carePlanBuilder.build());
    }

}
