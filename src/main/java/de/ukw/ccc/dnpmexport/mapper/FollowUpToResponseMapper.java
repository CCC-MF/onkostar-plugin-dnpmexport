/*
 * MIT License
 *
 * Copyright (c) 2024 Comprehensive Cancer Center Mainfranken
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
import de.ukw.ccc.bwhc.dto.Response;
import de.ukw.ccc.bwhc.dto.Value;

import java.util.Optional;

public class FollowUpToResponseMapper extends FollowUpMapper<Optional<Response>> {

    static final String FIELD_NAME_DATUM = "DatumFollowUp";
    static final String FIELD_NAME_BEST_RESPONSE = "BestResponse";
    static final String FIELD_NAME_THERAPY = "LinkTherapieempfehlung";

    public FollowUpToResponseMapper(final MapperUtils mapperUtils) {
        super(mapperUtils);
    }

    @Override
    public Optional<Response> apply(Procedure procedure) {
        if (!canApply(procedure)) {
            return Optional.empty();
        }

        var datum = procedure.getValue(FIELD_NAME_DATUM);
        if (null == datum || null == datum.getDate()) {
            return Optional.empty();
        }

        var therapyId = procedure.getValue(FIELD_NAME_THERAPY);
        if (null == therapyId || therapyId.getString().isBlank()) {
            return Optional.empty();
        }

        var bestResponse = procedure.getValue(FIELD_NAME_BEST_RESPONSE);
        if (null == bestResponse || mapValue(bestResponse.getString()).isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                Response.builder()
                        .withId(anonymizeId(procedure))
                        .withPatient(getPatientId(procedure))
                        .withEffectiveDate(dateFormat().format(datum.getDate()))
                        .withTherapy(anonymizeString(therapyId.getString()))
                        .withValue(mapValue(bestResponse.getString()).get())
                        .build()
        );
    }

    private static Optional<Value> mapValue(String value) {
        var valueBuilder = Value.builder();

        switch (value) {
            case "c":
                valueBuilder.withCode(Value.Recist.CR);
                break;
            case "t":
                valueBuilder.withCode(Value.Recist.PR);
                break;
            case "m":
                valueBuilder.withCode(Value.Recist.MR);
                break;
            case "s":
                valueBuilder.withCode(Value.Recist.SD);
                break;
            case "p":
                valueBuilder.withCode(Value.Recist.PD);
                break;
            case "n":
                valueBuilder.withCode(Value.Recist.NA);
                break;
            case "u":
            case "x":
            case "y":
            default:
                return Optional.empty();
        }

        return Optional.of(valueBuilder.build());
    }
}
