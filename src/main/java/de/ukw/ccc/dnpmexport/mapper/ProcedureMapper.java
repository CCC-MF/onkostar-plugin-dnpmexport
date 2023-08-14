/*
 * This file is part of ETL-Processor
 *
 * Copyright (c) 2023  Comprehensive Cancer Center Mainfranken, Datenintegrationszentrum Philipps-Universität Marburg and Contributors
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
 */

package de.ukw.ccc.dnpmexport.mapper;

import de.itc.onkostar.api.Procedure;

public abstract class ProcedureMapper<D> implements Mapper<Procedure, D> {

    protected final MapperUtils mapperUtils;

    public ProcedureMapper(final MapperUtils mapperUtils) {
        this.mapperUtils = mapperUtils;
    }

    protected String anonymizeId(Procedure procedure) {
        return mapperUtils.anonymizeId(procedure.getId().toString());
    }

    protected static String getPatientId(Procedure procedure) {
        return MapperUtils.getPatientId(procedure.getPatient());
    }

}
