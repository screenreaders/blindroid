/*
 * Copyright (C) 2016 The Soft Braille Keyboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.screenreaders.blindroid.braillekeyboard;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.eyesfree.braille.translate.TableInfo;
import com.googlecode.eyesfree.braille.translate.TranslatorClient;

// TODO(ddalton): Find a way to use the service directly without this class.
public class MyTranslatorClient extends TranslatorClient {
    public MyTranslatorClient(Context context, OnInitListener onInitListener) {
        mContext = context;
        mOnInitListener = onInitListener;
        LiblouisBridge.init(context);
        initTables(buildTables(context));
        notifyInit(SUCCESS);
    }

    private static List<TableInfo> buildTables(Context context) {
        String[] ids = context.getResources().getStringArray(
                R.array.braille_tables);
        List<TableInfo> tables = new ArrayList<TableInfo>(ids.length);
        for (String id : ids) {
            tables.add(new TableInfo(
                    id,
                    parseLocale(id),
                    isEightDot(id),
                    parseGrade(id)));
        }
        return tables;
    }
}
