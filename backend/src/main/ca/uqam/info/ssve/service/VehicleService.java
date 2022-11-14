package ca.uqam.info.ssve.service;

import ca.uqam.info.ssve.model.*;
import ca.uqam.info.ssve.repository.VehicleRepository;
import ca.uqam.info.ssve.server.ADVEConnection;
import com.jcraft.jsch.JSchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;
    private ADVEConnection adveConnection = new ADVEConnection();

    public Vehicle getVehicle(Long id) {
        return vehicleRepository.findById(id).get();
    }

    /**
     * @param vehicle
     * @return
     */
    public Vehicle addVehicle(Vehicle vehicle) {
        if (
                validateBrand(vehicle.getBrand())
                        && validateModelName(vehicle.getModelName())
                        && validatePrice(vehicle.getPrice())
                        && validateNbDoors(vehicle.getNbDoors())
                        && validateType(vehicle.getType())
                        && validateRange(vehicle.getRange())
                        && validateBatteryCapacity(vehicle.getBatteryCapacity())
                        && validateSafetyScore(vehicle.getSafetyScore())
                        && validateRefLink(vehicle.getRefLink())
                        && validateImgLink(vehicle.getImgLink())
        ) {
            long size = vehicleRepository.count();
            vehicleRepository.save(vehicle);
            Optional<Vehicle> voiture = vehicleRepository.findById(size + 1);
            if (voiture.isPresent()) {
                return voiture.get();
            }
            throw new IllegalArgumentException();
        }
        throw new IllegalArgumentException();
    }

    public List<Vehicle> getAllVehicle() {
        return vehicleRepository.findAll();
    }

    public Vehicle modifyVehicle(Vehicle vehicle) {
        if (
                validateBrand(vehicle.getBrand())
                        && validateModelName(vehicle.getModelName())
                        && validatePrice(vehicle.getPrice())
                        && validateNbDoors(vehicle.getNbDoors())
                        && validateType(vehicle.getType())
                        && validateRange(vehicle.getRange())
                        && validateBatteryCapacity(vehicle.getBatteryCapacity())
                        && validateSafetyScore(vehicle.getSafetyScore())
                        && validateRefLink(vehicle.getRefLink())
                        && validateImgLink(vehicle.getImgLink())
                        && vehicleRepository.findById(vehicle.getId()).isPresent()
        ) {
            vehicleRepository.save(vehicle);
            return vehicleRepository.findById(vehicle.getId()).get();
        }
        throw new IllegalArgumentException();
    }


    // ----------------------------------------------------   ----------------------------------------
    private boolean validateBrand(String brand) {
        return brand.matches("[a-zA-Z]+");
    }

    private boolean validateModelName(String modelName) {
        return modelName.matches("[A-Za-z\s0-9]+");
    }

    private boolean validateNbDoors(int nbDoors) {
        return nbDoors > 0 && nbDoors < 10;
    }

    private boolean validateType(String type) {
        return type.matches("[a-zA-Z]+");
    }

    private boolean validatePrice(int price) {
        return price > 0 && price < Integer.MAX_VALUE;
    }

    private boolean validateRange(int range) {
        return range > 0 && range < 2000;
    }

    private boolean validateBatteryCapacity(int batteryCapacity) {
        return batteryCapacity > 0 && batteryCapacity < Integer.MAX_VALUE;
    }

    private boolean validateSafetyScore(int safetyScore) {
        return safetyScore >= 0 && safetyScore <= 5;
    }

    private boolean validateRefLink(String refLink) {
        return refLink.matches("(\\b(https?|ftp|file)://)?[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
    }


    private boolean validateImgLink(String imgLink) {
        return imgLink.matches("(\\b(https?|ftp|file)://)?[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
    }

    //--------------------------------- ---------------------------------------
    private boolean validateId(Long id) {
        return id > 0 && id < Integer.MAX_VALUE;
    }


    private boolean validateScore(double score) {
        return score >= 0.0 && score <= 15.0;
    }


    public List<Evaluation> evaluateVehicle(List<Deplacement> coordinateList) throws IOException, JSchException, InterruptedException {
        //-------- DÉBUT ALGO RÉEL---------
        ArrayList<Route> routeList = new ArrayList<>();
        ArrayList<Evaluation> vehicleFinalScore = new ArrayList<>();
        int frequenceTotale = 0;
        ADVEConnection adveConnection = new ADVEConnection();

        //--------Détermination de la fréquence total et du poid de chaque route
        for (Deplacement x : coordinateList) {
            Route route = new Route();
            route.setFrequence(x.getFd().getNb_days());
            route.setDeplacement(x);
            frequenceTotale += route.getFrequence();
            routeList.add(route);
        }
        for (Route route : routeList)
            route.setWeight(((double) route.getFrequence() / frequenceTotale) + (route.getFrequence() % frequenceTotale));

        //--------Évaluation de chaque route pour chaque voiture et calcule de la note final
        List<Vehicle> allVehicle = getAllVehicle();
        allVehicle.sort(Comparator.comparing(Vehicle::getRange));
        for (int i = 0; i < allVehicle.size(); i++) {
            double score = 0;
            for (Route route : routeList) {
                //--------Obtien les infos du déplacement avec la boite noir
                String data = adveConnection.doRequest(requeteString(route) + allVehicle.get(i).getRange());
                stringToRoute(route, data);
                //--------Donne une note au déplacement pour la voiture i
                evaluateRoute(route, allVehicle, i);
            }
            //-------- Calcule la note final de la voiture selon la note de chaque déplacement
            for (Route route : routeList)
                score = score + (route.getWeight() * route.getScore());

            //--------Ajoute le score final a la voiture et l'ajoute dans la liste a retourné
            Evaluation evaluation = new Evaluation(allVehicle.get(i));
            evaluation.setScore(score);
            vehicleFinalScore.add(evaluation);
        }
        //--------Sort les voitures par score
        vehicleFinalScore.sort(Comparator.comparing(Evaluation::getScore));
        return vehicleFinalScore;
    }

    private String requeteString(Route route) {
        String start =
                "(" + route.getDeplacement().getStart().getLat() + "," + route.getDeplacement().getStart().getLgt() + ")";
        String end =
                "(" + route.getDeplacement().getEnd().getLat() + "," + route.getDeplacement().getEnd().getLgt() + ")";
        return start + " " + end + " ";
    }


    private void evaluateRoute(Route route, List<Vehicle> vehicle, int i) {
        double poid1 = 0.75;
        double poid2 = 0.25;
        double note1 = (vehicle.get(i).getRange() / route.getDistance()) + (vehicle.get(i).getRange() % route.getDistance()) * 100;
        if (note1 > 100)
            note1 = 100;
        int rangeMax = vehicle.get(0).getRange();
        double note2 = ((vehicle.get(i).getRange() - route.getDistance()) / rangeMax) + (vehicle.get(i).getRange() % rangeMax) * 100;

        route.setScore(poid1 * note1 + poid2 * note2);
    }

    private List<String> createCoordinateList() {
        List<String> coordinateList = new ArrayList<>();
        coordinateList.add("(45.1138,-72.3623)      (45.5382,-73.9159)      125162");
        coordinateList.add("(48.0293,-71.7262)      (45.0393,-72.5376)      135982");
        coordinateList.add("(47.6861,-70.3343)      (48.2191,-68.9323)      162139");
        coordinateList.add("(46.2825,-76.1005)      (46.9882,-71.7642)      433260");
        coordinateList.add("(47.5552,-75.4722)      (48.7095,-65.8653)      282048");
        coordinateList.add("(48.1702,-68.2585)      (47.3529,-72.3948)      172035");
        coordinateList.add("(48.7013,-69.1475)      (45.758,-75.8012)       484741");
        coordinateList.add("(48.2694,-68.1651)      (48.3162,-70.9388)      354968");
        coordinateList.add("(45.4755,-73.8757)      (47.3163,-69.8303)      169796");
        coordinateList.add("(46.1248,-75.6846)      (45.1296,-71.5386)      394615");
        return coordinateList;
    }

    private List<Deplacement> createDeplacementList() {
        List<Deplacement> deplacementList = new ArrayList<>();
        deplacementList.add(new Deplacement(1, new PointGeo(45.1138, -72.3623), new PointGeo(45.5382, -73.9159), FrequenceDeplacement.WORKING_DAYS));
        deplacementList.add(new Deplacement(1, new PointGeo(48.0293, -71.7262), new PointGeo(45.0393, -72.5376), FrequenceDeplacement.EVERYDAY));
        deplacementList.add(new Deplacement(1, new PointGeo(47.6861, -70.3343), new PointGeo(48.2191, -68.9323), FrequenceDeplacement.ONCE_A_WEEK));
        deplacementList.add(new Deplacement(1, new PointGeo(46.2825, -76.1005), new PointGeo(46.9882, -71.7642), FrequenceDeplacement.TWICE_A_WEEK));
        deplacementList.add(new Deplacement(1, new PointGeo(47.5552, -75.4722), new PointGeo(48.7095, -65.8653), FrequenceDeplacement.ONCE_A_MONTH));
        deplacementList.add(new Deplacement(1, new PointGeo(48.1702, -68.2585), new PointGeo(47.3529, -72.3948), FrequenceDeplacement.TWICE_A_MONTH));
        deplacementList.add(new Deplacement(1, new PointGeo(48.7013, 69.1475), new PointGeo(45.758, -75.8012), FrequenceDeplacement.ONCE_A_YEAR));
        deplacementList.add(new Deplacement(1, new PointGeo(48.2694, -68.1651), new PointGeo(48.3162, -70.9388), FrequenceDeplacement.TWICE_A_YEAR));
        deplacementList.add(new Deplacement(1, new PointGeo(45.4755, -73.8757), new PointGeo(47.3163, -69.8303), FrequenceDeplacement.ONCE_A_WEEK));
        deplacementList.add(new Deplacement(1, new PointGeo(46.1248, -75.6846), new PointGeo(45.1296, -71.5386), FrequenceDeplacement.TWICE_A_WEEK));
        return deplacementList;
    }

    private void stringToRoute(Route route, String data) {
        int valueNum = 0;
        int start = 0;
        int end = 0;
        for (int i = 0; i < data.length(); i++) {
            if (data.charAt(i) == '<') {
                start = i + 1;
            } else if (data.charAt(i) == '>') {
                end = i - 1;
                switch (valueNum) {
                    case 0 -> {
                        route.setDistance(Double.parseDouble(data.substring(start, end)));
                        valueNum++;
                        start = 0;
                        end = 0;
                    }
                    case 1 -> {
                        route.setTripTime(Double.parseDouble(data.substring(start, end)));
                        valueNum++;
                        start = 0;
                        end = 0;
                    }
                    case 2 -> {
                        route.setWaitingTime(Double.parseDouble(data.substring(start, end)));
                        valueNum++;
                        start = 0;
                        end = 0;
                    }
                    case 3 -> {
                        route.setChargingTime(Double.parseDouble(data.substring(start, end)));
                        valueNum++;
                        start = 0;
                        end = 0;
                    }
                }
            }
        }
    }


    //Dummy pour FrontEnd    ------------------------------------------------------------------------------
    public List<Evaluation> dummyScore() {
        List<Vehicle> list = getAllVehicle();
        List<Evaluation> list2 = new ArrayList<>();
        for (Vehicle vehicle : list) {
            Evaluation eval = new Evaluation();
            eval.setId(vehicle.getId());
            eval.setBrand(vehicle.getBrand());
            eval.setModelName(vehicle.getModelName());
            eval.setNbDoors(vehicle.getNbDoors());
            eval.setType(vehicle.getType());
            eval.setPrice(vehicle.getPrice());
            eval.setRange(vehicle.getRange());
            eval.setBatteryCapacity(vehicle.getBatteryCapacity());
            eval.setSafetyScore(vehicle.getSafetyScore());
            eval.setRefLink(vehicle.getRefLink());
            eval.setImgLink(vehicle.getImgLink());
            list2.add(eval);
        }

        return list2;
    }

    public List<Evaluation> evaluateVehicleTest() throws IOException, JSchException, InterruptedException {
        List<Deplacement> coordinateList = createDeplacementList(); //DummyList erase when real ones comes
        adveConnection.connectServer();
        //-------- DÉBUT ALGO RÉEL---------
        ArrayList<Route> routeList = new ArrayList<>();
        ArrayList<Evaluation> vehicleFinalScore = new ArrayList<>();
        List<String> req = createCoordinateList();
        int frequenceTotale = 0;

        //--------Détermination de la fréquence total et du poid de chaque route
        for (Deplacement x : coordinateList) {
            Route route = new Route();
            route.setFrequence(x.getFd().getNb_days());
            route.setDeplacement(x);
            frequenceTotale += route.getFrequence();
            routeList.add(route);
        }
        for (Route route : routeList) {
            route.setWeight(((double) route.getFrequence() / frequenceTotale) + (route.getFrequence() % frequenceTotale));
        }

        //--------Évaluation de chaque route pour chaque voiture et calcule de la note final
        List<Vehicle> allVehicle = getAllVehicle();
        allVehicle.sort(Comparator.comparing(Vehicle::getRange));
        for (int i = 0; i < allVehicle.size(); i++) {
            double score = 0;
            for (Route route : routeList) {
                //--------Obtien les infos du déplacement avec la boite noir
                String data = adveConnection.doRequest(requeteString(route) + allVehicle.get(i).getRange());
                System.out.println(data);
                stringToRoute(route, data);
                //--------Donne une note au déplacement pour la voiture i
                evaluateRoute(route, allVehicle, i);
            }
            //-------- Calcule la note final de la voiture selon la note de chaque déplacement
            for (Route route : routeList)
                score = score + (route.getWeight() * route.getScore());
            //--------Ajoute le score final a la voiture et l'ajoute dans la liste a retourné
            Evaluation evaluation = new Evaluation(allVehicle.get(i));
            evaluation.setScore(score);
            vehicleFinalScore.add(evaluation);
        }
        adveConnection.closeServer();
        //--------Sort les voitures par score
        vehicleFinalScore.sort(Comparator.comparing(Evaluation::getScore));
        return vehicleFinalScore;
    }
}