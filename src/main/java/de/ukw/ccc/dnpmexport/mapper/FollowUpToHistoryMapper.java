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
import de.ukw.ccc.bwhc.dto.PeriodStartEnd;

import java.util.List;
import java.util.Optional;

public class FollowUpToHistoryMapper extends FollowUpMapper<Optional<History>> {

    static final String FIELD_NAME_RECORDED_ON = "DatumFollowUp";
    static final String FIELD_NAME_STATUS = "StatusTherapie";
    static final String FIELD_NAME_BASED_ON = "LinkTherapieempfehlung";
    static final String FIELD_NAME_PERIOD_START = "Therapiestart";
    static final String FIELD_NAME_PERIOD_END = "Therapieende";

    static final String FIELD_NAME_EINZELEMPFEHLUNG_MEDICATION_JSON = "wirkstoffejson";

    public FollowUpToHistoryMapper(final MapperUtils mapperUtils) {
        super(mapperUtils);
    }

    @Override
    public Optional<History> apply(Procedure procedure) {
        if (!canApply(procedure)) {
            return Optional.empty();
        }

        final var builder = History.builder()
                .withId(anonymizeId(procedure))
                .withPatient(getPatientId(procedure));

        var recordedOn = procedure.getValue(FIELD_NAME_RECORDED_ON);
        if (null != recordedOn) {
            builder.withRecordedOn(dateFormat().format(recordedOn.getDate()));
        }

        var status = procedure.getValue(FIELD_NAME_STATUS);
        if (null != status) {
            builder.withStatus(mapStatus(status.getString()));
        }

        var basedOn = procedure.getValue(FIELD_NAME_BASED_ON);
        if (null != basedOn && basedOn.getString().matches("[0-9]*")) {
            builder.withBasedOn(anonymizeString(basedOn.getString()));

            final var einzelempfehlung = mapperUtils.onkostarApi().getProcedure(Integer.parseInt(basedOn.getString()));
            if (null != einzelempfehlung) {
                final var wirkstoffeJson = einzelempfehlung.getValue(FIELD_NAME_EINZELEMPFEHLUNG_MEDICATION_JSON);
                if (null != wirkstoffeJson) {
                    builder.withMedication(new JsonToMedicationMapper().apply(wirkstoffeJson.getString()).orElse(List.of()));
                }
            }
        }

        var periodStart = procedure.getValue(FIELD_NAME_PERIOD_START);
        var periodEnd = procedure.getValue(FIELD_NAME_PERIOD_END);
        if (null != periodStart) {
            final var periodBuilder = PeriodStartEnd.builder().withStart(dateFormat().format(periodStart.getDate()));
            if (null != periodEnd) {
                periodBuilder.withEnd(dateFormat().format(periodEnd.getDate()));
            }
            builder.withPeriod(periodBuilder.build());
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
