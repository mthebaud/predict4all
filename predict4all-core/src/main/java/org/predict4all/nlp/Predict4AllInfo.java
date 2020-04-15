/*
 * Copyright 2020 - Mathieu THEBAUD
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.predict4all.nlp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

/**
 * This retrieves information about the library (version and build date).<br>
 * This should mostly be used to ensure consistency on saved data (i.e. save and load data from same versions)
 *
 * @author Mathieu THEBAUD
 */
public class Predict4AllInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(Predict4AllInfo.class);

    public static final String VERSION = getVersion();
    public static final Date BUILD_DATE = getBuildDate();

    private static String cachedVersion = null;
    private static Date cachedBuildDate = null;

    private static String getVersion() {
        if (cachedVersion == null) {
            loadInfo();
        }
        return cachedVersion;
    }

    private static Date getBuildDate() {
        if (cachedBuildDate == null) {
            loadInfo();
        }
        return cachedBuildDate;
    }

    private static void loadInfo() {
        Properties props = new Properties();
        try (InputStream is = Predict4AllInfo.class.getResourceAsStream("/predict4all.properties")) {
            props.load(is);
            cachedVersion = props.getProperty("version", "0.0");
            cachedBuildDate = new Date(Long.parseLong(props.getProperty("buildDate", "0")));
        } catch (Throwable t) {
            LOGGER.warn("Couldn't load Predict4All information from resource", t);
            //Ignored
        }
    }

}
