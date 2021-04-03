// Copyright 2021 ZeoFlow SRL
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.zeoflow.log;

import android.os.Handler;
import android.os.HandlerThread;

import com.zeoflow.annotation.NonNull;
import com.zeoflow.annotation.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static com.zeoflow.core.utils.Preconditions.checkNotNull;
import static com.zeoflow.initializer.ZeoFlowApp.getContext;

/**
 * CSV formatted file logging for Android.
 * Writes to CSV the following data:
 * epoch timestamp, ISO8601 timestamp (human-readable), log level, tag, log message.
 */
@SuppressWarnings({"unchecked", "RedundantSuppression"})
public class CsvFormatStrategy implements FormatStrategy
{

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String NEW_LINE_REPLACEMENT = " <br> ";
    private static final String SEPARATOR = ",";

    @NonNull
    private final Date date;
    @NonNull
    private final SimpleDateFormat dateFormat;
    @NonNull
    private final LogStrategy logStrategy;
    @Nullable
    private final String tag;

    private CsvFormatStrategy(@NonNull Builder builder)
    {
        checkNotNull(builder);

        date = builder.date;
        dateFormat = builder.dateFormat;
        logStrategy = builder.logStrategy;
        tag = builder.tag;
    }

    @NonNull
    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    public void log(int priority, @Nullable String onceOnlyTag, @NonNull String message)
    {
        checkNotNull(message);

        String tag = formatTag(onceOnlyTag);

        date.setTime(System.currentTimeMillis());

        StringBuilder builder = new StringBuilder();

        // machine-readable date/time
        builder.append(date.getTime());

        // human-readable date/time
        builder.append(SEPARATOR);
        builder.append(dateFormat.format(date));

        // level
        builder.append(SEPARATOR);
        builder.append(Utils.logLevel(priority));

        // tag
        builder.append(SEPARATOR);
        builder.append(tag);

        // message
        if (message.contains(NEW_LINE))
        {
            // a new line would break the CSV format, so we replace it here
            message = message.replaceAll(NEW_LINE, NEW_LINE_REPLACEMENT);
        }
        builder.append(SEPARATOR);
        builder.append(message);

        // new line
        builder.append(NEW_LINE);

        logStrategy.log(priority, tag, builder.toString());
    }

    @Nullable
    private String formatTag(@Nullable String tag)
    {
        if (!Utils.isEmpty(tag) && !Utils.equals(this.tag, tag))
        {
            return this.tag + "-" + tag;
        }
        return this.tag;
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static final class Builder
    {
        private static final int MAX_BYTES = 500 * 1024; // 500K averages to a 4000 lines per file

        Date date;
        SimpleDateFormat dateFormat;
        LogStrategy logStrategy;
        String tag = "LOGGER";

        private Builder()
        {
        }

        @NonNull
        public Builder date(@Nullable Date val)
        {
            date = val;
            return this;
        }

        @NonNull
        public Builder dateFormat(@Nullable SimpleDateFormat val)
        {
            dateFormat = val;
            return this;
        }

        @NonNull
        public Builder logStrategy(@Nullable LogStrategy val)
        {
            logStrategy = val;
            return this;
        }

        @NonNull
        public Builder tag(@Nullable String tag)
        {
            this.tag = tag;
            return this;
        }

        @NonNull
        public CsvFormatStrategy build()
        {
            if (date == null)
            {
                date = new Date();
            }
            if (dateFormat == null)
            {
                dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.UK);
            }
            if (logStrategy == null)
            {
                String diskPath = Objects.requireNonNull(getContext().getExternalFilesDir(null)).getAbsolutePath();
                String folder = diskPath + File.separatorChar + "log";

                HandlerThread ht = new HandlerThread("AndroidFileLogger." + folder);
                ht.start();
                Handler handler = new DiskLogStrategy.WriteHandler(ht.getLooper(), folder, MAX_BYTES);
                logStrategy = new DiskLogStrategy(handler);
            }
            return new CsvFormatStrategy(this);
        }
    }
}
