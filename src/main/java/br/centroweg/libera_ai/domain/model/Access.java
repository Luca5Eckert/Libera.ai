package br.centroweg.libera_ai.domain.model;

import java.time.LocalDateTime;

public class Access
{

    private final int id;

    private final int code;

    private final LocalDateTime entry;

    private LocalDateTime exit;

    public Access(int id, int code, LocalDateTime entry, LocalDateTime exit) {
        this.id = id;
        this.code = code;
        this.entry = entry;
        this.exit = exit;
    }

    public int getId() {
        return id;
    }

    public int getCode() {
        return code;
    }

    public LocalDateTime getEntry() {
        return entry;
    }

    public LocalDateTime getExit() {
        return exit;
    }

    public void setExit(LocalDateTime exit) {
        this.exit = exit;
    }

}
