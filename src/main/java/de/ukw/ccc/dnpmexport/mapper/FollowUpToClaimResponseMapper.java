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
import de.ukw.ccc.bwhc.dto.ClaimResponse;

import java.util.Optional;

public class FollowUpToClaimResponseMapper extends FollowUpMapper<Optional<ClaimResponse>> {

    static final String FIELD_NAME_ISSUED_ON = "Datum_AntwortKueAntrag";
    static final String FIELD_NAME_STATUS = "StatusKostenuebernahme";
    static final String FIELD_NAME_REASON = "AblehnungKosten";

    public FollowUpToClaimResponseMapper(final MapperUtils mapperUtils) {
        super(mapperUtils);
    }

    @Override
    public Optional<ClaimResponse> apply(Procedure procedure) {
        if (!canApply(procedure)) {
            return Optional.empty();
        }

        var issuedOn = procedure.getValue(FIELD_NAME_ISSUED_ON).getDate();
        var status = procedure.getValue(FIELD_NAME_STATUS).getString();
        var reason = procedure.getValue(FIELD_NAME_REASON).getString();

        final var builder = ClaimResponse.builder()
                .withId(anonymizeId(procedure))
                .withPatient(getPatientId(procedure))
                // Uses same ID for claim and claim response
                .withClaim(anonymizeId(procedure));

        if (null != issuedOn) {
            builder.withIssuedOn(dateFormat().format(issuedOn));
        }

        if (null != status) {
            builder.withStatus(mapStatus(status));
        }

        if (null != reason) {
            builder.withReason(mapReason(reason));
        }

        return Optional.of(builder.build());
    }

    private static ClaimResponse.ClaimStatus mapStatus(String value) {
        switch (value) {
            case "accepted":
                return ClaimResponse.ClaimStatus.ACCEPTED;
            case "rejected":
                return ClaimResponse.ClaimStatus.REJECTED;
            default:
                return null;
        }
    }

    private static ClaimResponse.Reason mapReason(String value) {
        switch (value) {
            case "e":
                return ClaimResponse.Reason.INSUFFICIENT_EVIDENCE;
            case "s":
                return ClaimResponse.Reason.STANDARD_THERAPY_NOT_EXHAUSTED;
            case "w":
                return ClaimResponse.Reason.OTHER;
            default:
                return null;
        }
    }
}
