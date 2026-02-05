package com.application.canopy.model;

public class TimerPreset {
    private int id;
    private String name;
    private int focusMinutes;
    private int shortBreakMinutes;
    private int longBreakMinutes;
    private int repeatBeforeLongBreak; // quante volte ripetere focus+pausa breve prima della pausa lunga

    public TimerPreset(int id,
                       String name,
                       int focusMinutes,
                       int shortBreakMinutes,
                       int longBreakMinutes,
                       int repeatBeforeLongBreak) {
        this.id = id;
        this.name = name;
        this.focusMinutes = focusMinutes;
        this.shortBreakMinutes = shortBreakMinutes;
        this.longBreakMinutes = longBreakMinutes;
        this.repeatBeforeLongBreak = repeatBeforeLongBreak;
    }

    public TimerPreset(String name,
                       int focusMinutes,
                       int shortBreakMinutes,
                       int longBreakMinutes,
                       int repeatBeforeLongBreak) {
        this(0, name, focusMinutes, shortBreakMinutes, longBreakMinutes, repeatBeforeLongBreak);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getFocusMinutes() { return focusMinutes; }
    public void setFocusMinutes(int focusMinutes) { this.focusMinutes = focusMinutes; }

    public int getShortBreakMinutes() { return shortBreakMinutes; }
    public void setShortBreakMinutes(int shortBreakMinutes) { this.shortBreakMinutes = shortBreakMinutes; }

    public int getLongBreakMinutes() { return longBreakMinutes; }
    public void setLongBreakMinutes(int longBreakMinutes) { this.longBreakMinutes = longBreakMinutes; }

    public int getRepeatBeforeLongBreak() { return repeatBeforeLongBreak; }
    public void setRepeatBeforeLongBreak(int repeatBeforeLongBreak) {
        this.repeatBeforeLongBreak = repeatBeforeLongBreak;
    }
}
