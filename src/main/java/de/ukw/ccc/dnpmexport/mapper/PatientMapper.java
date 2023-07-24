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

import de.itc.onkostar.api.Patient;
import de.itc.onkostar.api.Sex;

import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.function.Function;

import static de.ukw.ccc.dnpmexport.mapper.MapperUtils.getPatientId;

public class PatientMapper implements Function<Patient, Optional<de.ukw.ccc.bwhc.dto.Patient>> {

    @Override
    public Optional<de.ukw.ccc.bwhc.dto.Patient> apply(Patient patient) {
        if (null == patient.getBirthdate()) {
            return Optional.empty();
        }

        var formatter = new SimpleDateFormat("yyyy-MM");

        var patientBuilder = de.ukw.ccc.bwhc.dto.Patient.builder()
                .withId(getPatientId(patient))
                .withBirthDate(formatter.format(patient.getBirthdate()))
                .withGender(map(patient.getSex()));

        if (null != patient.getDeathdate()) {
            patientBuilder.withDateOfDeath(formatter.format(patient.getDeathdate()));
        }

        return Optional.of(patientBuilder.build());
    }

    private de.ukw.ccc.bwhc.dto.Patient.Gender map(Sex sex) {
        switch (sex) {
            case MALE:
                return de.ukw.ccc.bwhc.dto.Patient.Gender.MALE;
            case FEMALE:
                return de.ukw.ccc.bwhc.dto.Patient.Gender.FEMALE;
            case UNKNOWN:
                return de.ukw.ccc.bwhc.dto.Patient.Gender.UNKNOWN;
            default:
                return de.ukw.ccc.bwhc.dto.Patient.Gender.OTHER;
        }
    }
}
