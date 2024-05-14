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
import de.ukw.ccc.bwhc.dto.History;

import java.util.Optional;

public class FollowUpToHistoryMapper extends FollowUpMapper<Optional<History>> {

    static final String FIELD_NAME_RECORDED_ON = "DatumFollowUp";
    static final String FIELD_NAME_STATUS = "StatusTherapie";
    static final String FIELD_NAME_BASED_ON = "LinkTherapieempfehlung";

    public FollowUpToHistoryMapper(final MapperUtils mapperUtils) {
        super(mapperUtils);
    }

    @Override
    public Optional<History> apply(Procedure procedure) {
        if (!canApply(procedure)) {
            return Optional.empty();
        }

        var recordedOn = procedure.getValue(FIELD_NAME_RECORDED_ON).getDate();
        var status = procedure.getValue(FIELD_NAME_STATUS).getString();
        var basedOn = procedure.getValue(FIELD_NAME_BASED_ON).getString();

        final var builder = History.builder()
                .withId(anonymizeId(procedure))
                .withPatient(getPatientId(procedure));

        if (null != recordedOn) {
            builder.withRecordedOn(dateFormat().format(recordedOn));
        }

        if (null != status) {
            builder.withStatus(mapStatus(status));
        }

        if (null != basedOn) {
            builder.withBasedOn(anonymizeString(basedOn));
        }

        return Optional.of(builder.build());
    }

    private static History.MolecularTherapyStatus mapStatus(String value) {
        switch (value) {
            case "not-done":
                return History.MolecularTherapyStatus.NOT_DONE;
            case "on-going":
                return History.MolecularTherapyStatus.ON_GOING;
            case "stopped":
                return History.MolecularTherapyStatus.STOPPED;
            case "completed":
                return History.MolecularTherapyStatus.COMPLETED;
            default:
                return null;
        }
    }
}
