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
import de.itc.onkostar.api.ProcedureEditStateType;
import de.ukw.ccc.bwhc.dto.HistologyReevaluationRequest;

import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.function.Function;

import static de.ukw.ccc.dnpmexport.mapper.MapperUtils.getPatientId;

public class TherapieplanToHistologyReevaluationRequestMapper implements Function<Procedure, Optional<HistologyReevaluationRequest>> {

    private final MapperUtils mapperUtils;

    public TherapieplanToHistologyReevaluationRequestMapper(final MapperUtils mapperUtils) {
        this.mapperUtils = mapperUtils;
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

        var builder = HistologyReevaluationRequest.builder()
                .withId(mapperUtils.anonymizeId(procedure.getId().toString()))
                .withPatient(getPatientId(procedure))
                .withIssuedOn(formatter.format(procedure.getStartDate()));

        var probe = mapperUtils.onkostarApi().getProcedure(procedure.getValue("refreevaltumorprobe").getInt());
        if (null != probe && probe.getId() > 0 && probe.getEditState() == ProcedureEditStateType.COMPLETED) {
            builder.withSpecimen(mapperUtils.anonymizeId(probe.getId().toString()));
            return Optional.of(builder.build());
        }

        return Optional.empty();
    }

}
