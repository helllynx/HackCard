package org.hack.card;

import android.content.Context;
import android.widget.ArrayAdapter;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DumpListAdapter extends ArrayAdapter<DumpListAdapter.DumpListFilename> {
    private static Pattern pattern;

    private Pattern getPattern() {
        if (pattern == null) {
            pattern = Pattern.compile(Dump.FILENAME_REGEXP);
        }
        return pattern;
    }

    public class DumpListFilename {

        String filename;

        DumpListFilename(String filename) {
            this.filename = filename;
        }

        String getFilename() {
            return filename;
        }

        @NotNull
        public String toString() {
            Matcher m = getPattern().matcher(filename);
            if (m.matches()) {
                String info = "";
                Date d = new Date(
                        Integer.parseInt(Objects.requireNonNull(m.group(1))) - 1900,
                        Integer.parseInt(Objects.requireNonNull(m.group(2))) - 1,
                        Integer.parseInt(Objects.requireNonNull(m.group(3))),
                        Integer.parseInt(Objects.requireNonNull(m.group(4)).substring(0, 2)),
                        Integer.parseInt(Objects.requireNonNull(m.group(4)).substring(2, 4)),
                        Integer.parseInt(Objects.requireNonNull(m.group(4)).substring(4, 6))
                );
                info += DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(d);
                info += "\nCard: " + Dump.formatCardNumber(Integer.parseInt(m.group(5))) + "  -  RUB: " + m.group(6);
                return info;
            } else {
                return "error parsing filename";
            }
        }
    }

    DumpListAdapter(Context context, String[] filenames) {
        super(context, R.layout.dump_list_item, R.id.dump_list_item_label);

        Arrays.sort(filenames);
        List<String> filenamesList = Arrays.asList(filenames);
        Collections.reverse(filenamesList);

        for (String filename : filenamesList) {
            add(new DumpListFilename(filename));
        }
    }

}
