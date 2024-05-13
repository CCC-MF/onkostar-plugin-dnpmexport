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

import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;

import static de.ukw.ccc.dnpmexport.mapper.FollowUpToClaimMapper.FIELD_NAME_ISSUED_ON;
import static de.ukw.ccc.dnpmexport.mapper.FollowUpToClaimMapper.FIELD_NAME_THERAPY;
import static de.ukw.ccc.dnpmexport.test.TestUtils.createFollowUpProcedure;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class FollowUpToClaimMapperTest {

    private IOnkostarApi onkostarApi;
    private FollowUpToClaimMapper mapper;

    @BeforeEach
    void setUp(
            @Mock IOnkostarApi onkostarApi
    ) {
        this.onkostarApi = onkostarApi;
        this.mapper = new FollowUpToClaimMapper(new MapperUtils(onkostarApi));
    }

    @Test
    void shouldMapToClaim() {
        var procedure = createFollowUpProcedure(this.onkostarApi);
        procedure.setValue(FIELD_NAME_ISSUED_ON, new Item(FIELD_NAME_ISSUED_ON, Date.from(Instant.parse("2024-05-13T12:00:00Z"))));
        procedure.setValue(FIELD_NAME_THERAPY, new Item(FIELD_NAME_THERAPY, "1234"));

        var claim = this.mapper.apply(procedure);

        assertThat(claim).isNotEmpty();
        assertThat(claim.get().getId()).matches("UNKNOWN[a-z0-9]+");
        assertThat(claim.get().getPatient()).isEqualTo("2000123456");
        assertThat(claim.get().getIssuedOn()).isEqualTo("2024-05-13");
        assertThat(claim.get().getTherapy()).matches("UNKNOWN[a-z0-9]+");
    }

}
