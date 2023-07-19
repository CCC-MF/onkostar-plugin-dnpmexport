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

import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Procedure;
import de.ukw.ccc.bwhc.dto.HistologyReevaluationRequest;

import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.function.Function;

public class TherapieplanToHistologyReevaluationRequestMapper implements Function<Procedure, Optional<HistologyReevaluationRequest>> {

    private final IOnkostarApi onkostarApi;

    public TherapieplanToHistologyReevaluationRequestMapper(final IOnkostarApi onkostarApi) {
        this.onkostarApi = onkostarApi;
    }

    @Override
    public Optional<HistologyReevaluationRequest> apply(Procedure procedure) {
        if (null == procedure || !procedure.getFormName().equals("DNPM Therapieplan")) {
            return Optional.empty();
        }

        if (procedure.getDiseases().size() != 1) {
            return Optional.empty();
        }

        var formatter = new SimpleDateFormat("yyyy-MM-dd");

        return Optional.of(
                HistologyReevaluationRequest.builder()
                        .withId(procedure.getId().toString())
                        .withPatient(procedure.getPatient().getId().toString())
                        .withIssuedOn(formatter.format(procedure.getStartDate()))
                        .withSpecimen(procedure.getValue("refreevaltumorprobe").getString())
                        .build()
        );
    }

}