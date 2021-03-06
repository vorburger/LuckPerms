/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.utils;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class PasteUtils {

    public static String paste(String name, String desc, String contents) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.github.com/gists").openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            
            try (OutputStream os = connection.getOutputStream()) {
                StringWriter sw = new StringWriter();
                new JsonWriter(sw).beginObject()
                        .name("description").value(desc)
                        .name("public").value(false)
                        .name("files")
                        .beginObject().name(name)
                        .beginObject().name("content").value(contents)
                        .endObject()
                        .endObject()
                        .endObject();

                os.write(sw.toString().getBytes());
            }

            if (connection.getResponseCode() >= 400) {
                return null;
            }

            JsonObject response = new Gson().fromJson(new InputStreamReader(connection.getInputStream(), Charsets.UTF_8), JsonObject.class);
            String pasteUrl = response.get("html_url").getAsString();
            connection.disconnect();

            try {
                connection = (HttpURLConnection) new URL("https://git.io").openConnection();
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(("url=" + pasteUrl).getBytes());
                }
                pasteUrl = connection.getHeaderField("Location");
                connection.disconnect();
            } catch (Exception e) {
                // ignored
            }

            return pasteUrl;
        } catch (Exception e) {
            return null;
        }
    }
}
