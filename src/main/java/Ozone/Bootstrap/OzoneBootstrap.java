/*
 * Copyright 2021 Itzbenz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package Ozone.Bootstrap;

import Ozone.Settings.BaseSettings;
import Ozone.Version;
import Shared.SharedBoot;
import arc.util.Log;
import mindustry.Vars;

public class OzoneBootstrap {
	public static void init() {
		if (Vars.android) SharedBoot.type = "Ozone-Android";
		SharedBoot.initSentry();
		if (SharedBoot.debug) {
			Log.level = Log.LogLevel.debug;
			BaseSettings.debugMode = true;
			
		}
		Log.info("Ozone-Version: " + Version.core);
	}
}
