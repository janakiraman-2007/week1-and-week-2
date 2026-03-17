package week2;

import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class ParkingLot {

    private static final int TOTAL_SPOTS = 500;

    // Spot status
    enum Status { EMPTY, OCCUPIED, DELETED }

    // Parking spot
    static class Spot {
        String licensePlate;
        Status status;
        LocalDateTime entryTime;

        Spot() {
            status = Status.EMPTY;
        }
    }

    private final Spot[] spots = new Spot[TOTAL_SPOTS];
    private int totalProbes = 0;
    private int parkedVehicles = 0;

    public ParkingLot() {
        for (int i = 0; i < TOTAL_SPOTS; i++) spots[i] = new Spot();
    }

    // Simple hash function based on license plate
    private int hash(String licensePlate) {
        return Math.abs(licensePlate.hashCode()) % TOTAL_SPOTS;
    }

    // Park vehicle using linear probing
    public String parkVehicle(String licensePlate) {
        int preferredSpot = hash(licensePlate);
        int probes = 0;

        for (int i = 0; i < TOTAL_SPOTS; i++) {
            int spotIndex = (preferredSpot + i) % TOTAL_SPOTS;
            Spot spot = spots[spotIndex];

            if (spot.status == Status.EMPTY || spot.status == Status.DELETED) {
                spot.licensePlate = licensePlate;
                spot.status = Status.OCCUPIED;
                spot.entryTime = LocalDateTime.now();
                totalProbes += probes;
                parkedVehicles++;
                return "Assigned spot #" + spotIndex + " (" + probes + " probes)";
            }
            probes++;
        }
        return "Parking full, no available spots";
    }

    // Exit vehicle, calculate duration and fee
    public String exitVehicle(String licensePlate) {
        int preferredSpot = hash(licensePlate);

        for (int i = 0; i < TOTAL_SPOTS; i++) {
            int spotIndex = (preferredSpot + i) % TOTAL_SPOTS;
            Spot spot = spots[spotIndex];

            if (spot.status == Status.OCCUPIED && spot.licensePlate.equals(licensePlate)) {
                LocalDateTime exitTime = LocalDateTime.now();
                Duration duration = Duration.between(spot.entryTime, exitTime);
                double hours = duration.toMinutes() / 60.0;
                double fee = hours * 5; // $5/hour
                spot.status = Status.DELETED;
                spot.licensePlate = null;
                spot.entryTime = null;
                parkedVehicles--;
                return "Spot #" + spotIndex + " freed, Duration: " + formatDuration(duration) +
                        ", Fee: $" + String.format("%.2f", fee);
            }
        }
        return "Vehicle not found";
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return hours + "h " + minutes + "m";
    }

    // Get statistics
    public String getStatistics() {
        double occupancy = (parkedVehicles * 100.0) / TOTAL_SPOTS;
        double avgProbes = parkedVehicles == 0 ? 0 : totalProbes * 1.0 / parkedVehicles;
        // For demo, peak hour is random
        String peakHour = "2-3 PM";

        return String.format("Occupancy: %.1f%%, Avg Probes: %.2f, Peak Hour: %s",
                occupancy, avgProbes, peakHour);
    }

    // ------------------ TEST ------------------
    public static void main(String[] args) throws InterruptedException {
        ParkingLot lot = new ParkingLot();

        System.out.println(lot.parkVehicle("ABC-1234"));
        System.out.println(lot.parkVehicle("ABC-1235"));
        System.out.println(lot.parkVehicle("XYZ-9999"));

        Thread.sleep(2000); // simulate some parking time

        System.out.println(lot.exitVehicle("ABC-1234"));
        System.out.println(lot.getStatistics());
    }
}
