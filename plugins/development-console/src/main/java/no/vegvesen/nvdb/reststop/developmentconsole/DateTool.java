/*
 * Copyright 2018 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.vegvesen.nvdb.reststop.developmentconsole;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 */
public class DateTool {

    private SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private SimpleDateFormat todayFormat = new SimpleDateFormat("HH:mm:ss");

    public String formatTime(long time) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        Date date = new Date(time);
        Date startOfDay = cal.getTime();
        if(date.before(startOfDay)) {
            return dayFormat.format(date);
        } else {
            return todayFormat.format(date);
        }
    }
}
