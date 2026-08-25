 package co.wethinkcode.logisticsconnect.model;

import java.util.List;

public record HubDto(
        String hubId,
        String province,
        String sortingCenter,
        Boolean active,
        List<String> notes) {

    public HubDto {
        notes = List.copyOf(notes);
    }
}
