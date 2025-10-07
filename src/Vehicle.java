public class Vehicle {
    public String number;
    public long inTime; // epoch seconds
    public String assignedParkingId; // parking id if assigned

    public Vehicle(String number) {
        this.number = number.toUpperCase();
        this.inTime = System.currentTimeMillis() / 1000;
        this.assignedParkingId = null;
    }
}
