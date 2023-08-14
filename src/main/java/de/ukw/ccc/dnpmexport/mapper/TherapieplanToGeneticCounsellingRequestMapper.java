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
import de.ukw.ccc.bwhc.dto.GeneticCounsellingRequest;

import java.util.Optional;

import static de.ukw.ccc.dnpmexport.mapper.MapperUtils.getPatientId;

public class TherapieplanToGeneticCounsellingRequestMapper extends ProcedureMapper<Optional<GeneticCounsellingRequest>> {

    public TherapieplanToGeneticCounsellingRequestMapper(final MapperUtils mapperUtils) {
        super(mapperUtils);
    }

    @Override
    public Optional<GeneticCounsellingRequest> apply(Procedure procedure) {
        if (null == procedure || !procedure.getFormName().equals("DNPM Therapieplan")) {
            return Optional.empty();
        }

        if (procedure.getDiseases().size() != 1) {
            return Optional.empty();
        }

        return Optional.of(
                GeneticCounsellingRequest.builder()
                        .withId(anonymizeId(procedure))
                        .withPatient(getPatientId(procedure))
                        .withIssuedOn(dateFormat().format(procedure.getStartDate()))
                        .withReason(procedure.getValue("humangenberbegruendung").getString())
                        .build()
        );
    }

}
