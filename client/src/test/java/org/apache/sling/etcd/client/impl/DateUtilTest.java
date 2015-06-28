/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.etcd.client.impl;

import java.util.Calendar;

import junit.framework.Assert;
import org.junit.Test;

public class DateUtilTest {

    @Test
    public void testParseDate() throws Exception {
        Calendar calendar = DateUtil.parseDate("2015-03-28T03:10:35.166069176+01:00");
        Assert.assertNotNull(calendar);
        Assert.assertEquals(1427674704176L, calendar.getTime().getTime());
    }
}