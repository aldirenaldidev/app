package com.flexurio.shiftku;

/** Model shift. */
public class Shift {
    public long id;
    public String name;
    public String start; // "07:00"
    public String end;   // "15:00"
    public int color;
    public boolean dayOff; // libur/off: tidak dihitung jam kerja

    public Shift(long id, String name, String start, String end, int color, boolean dayOff) {
        this.id = id;
        this.name = name;
        this.start = start;
        this.end = end;
        this.color = color;
        this.dayOff = dayOff;
    }

    public String jam() {
        if (dayOff || start == null || start.isEmpty()) return "-";
        return start + " - " + end;
    }

    /** Durasi jam kerja, menangani shift malam yang lewat tengah malam. */
    public double hours() {
        if (dayOff || start == null || start.isEmpty()) return 0;
        try {
            String[] a = start.split(":");
            String[] b = end.split(":");
            int s = Integer.parseInt(a[0]) * 60 + Integer.parseInt(a[1]);
            int e = Integer.parseInt(b[0]) * 60 + Integer.parseInt(b[1]);
            if (e <= s) e += 24 * 60;
            return (e - s) / 60.0;
        } catch (Exception ex) { return 0; }
    }

    public String initial() {
        return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
    }
}
