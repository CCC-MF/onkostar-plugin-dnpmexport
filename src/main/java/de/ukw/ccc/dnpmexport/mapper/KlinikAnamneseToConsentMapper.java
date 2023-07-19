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
import de.ukw.ccc.bwhc.dto.Consent;

import java.util.Optional;
import java.util.function.Function;

import static de.ukw.ccc.dnpmexport.mapper.MapperUtils.getPatientId;

public class KlinikAnamneseToConsentMapper implements Function<Procedure, Optional<Consent>> {

    @Override
    public Optional<Consent> apply(Procedure procedure) {
        if (null == procedure || !procedure.getFormName().equals("DNPM Klinik/Anamnese")) {
            return Optional.empty();
        }

        var consentStatus = procedure.getValue("ConsentStatusEinwilligungDNPM").getString();

        if (null != consentStatus && consentStatus.equals("active")) {
            return Optional.of(
                    Consent.builder()
                            .withId(procedure.getId().toString())
                            .withPatient(getPatientId(procedure))
                            .withStatus(Consent.Status.ACTIVE)
                            .build()
            );
        }

        return Optional.of(
                Consent.builder()
                        .withId(procedure.getId().toString())
                        .withPatient(getPatientId(procedure))
                        .withStatus(Consent.Status.REJECTED)
                        .build()
        );
    }

}
