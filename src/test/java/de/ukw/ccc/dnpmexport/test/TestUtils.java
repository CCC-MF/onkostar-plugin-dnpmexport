/*
 * This file is part of ETL-Processor
 *
 * Copyright (C) 2024  Comprehensive Cancer Center Mainfranken, Datenintegrationszentrum Philipps-Universität Marburg and Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package de.ukw.ccc.dnpmexport.test;

import de.itc.onkostar.api.*;

import java.time.Instant;
import java.util.Date;

public class TestUtils {

    public static Disease createDisease(IOnkostarApi onkostarApi) {
        var disease = new Disease(onkostarApi);
        disease.setId(1);
        disease.setPatientId(createPatient(onkostarApi).getId());
        disease.setIcd10Code("C00.0");
        disease.setDiagnosisDate(new Date());

        return disease;
    }

    public static Procedure createKlinikAnamneseProcedure(IOnkostarApi onkostarApi) {
        var procedure = new Procedure(onkostarApi);
        procedure.setId(1);
        procedure.setFormName("DNPM Klinik/Anamnese");
        procedure.setPatient(createPatient(onkostarApi));
        procedure.addDisease(createDisease(onkostarApi));

        procedure.setValue("ConsentStatusEinwilligungDNPM", new Item("ConsentStatusEinwilligungDNPM", "rejected"));
        procedure.setValue("AnmeldedatumMTB", new Item("AnmeldedatumMTB", "2024-01-01"));

        return procedure;
    }

    public static Procedure createFollowUpProcedure(IOnkostarApi onkostarApi) {
        var procedure = new Procedure(onkostarApi);
        procedure.setId(1);
        procedure.setFormName("DNPM FollowUp");
        procedure.setPatient(createPatient(onkostarApi));
        procedure.addDisease(createDisease(onkostarApi));

        return procedure;
    }

    public static Patient createPatient(IOnkostarApi onkostarApi) {
        var patient = new Patient(onkostarApi);
        patient.setId(123456);
        patient.setPatientId("2000123456");
        patient.setGivenName("Patrick");
        patient.setFamilyName("Tester");
        patient.setBirthdate(Date.from(Instant.parse("2000-01-01T00:00:00Z")));
        patient.setSex(Sex.UNKNOWN);

        return patient;
    }
}
