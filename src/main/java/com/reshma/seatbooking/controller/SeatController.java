package com.reshma.seatbooking.controller;

import com.reshma.seatbooking.entity.Seat;
import com.reshma.seatbooking.service.SeatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping
    public List<Seat> list(@RequestParam(required = false) String show) {
        return seatService.list(show);
    }

    @GetMapping("/{id}")
    public Seat get(@PathVariable Long id) {
        return seatService.get(id);
    }

    @PostMapping("/{id}/hold")
    public Seat hold(@PathVariable Long id, @RequestParam String user) {
        return seatService.hold(id, user);
    }
}
