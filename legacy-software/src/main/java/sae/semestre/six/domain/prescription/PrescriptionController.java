package sae.semestre.six.domain.prescription;

import sae.semestre.six.domain.patient.PatientDao;
import sae.semestre.six.domain.patient.Patient;
import sae.semestre.six.domain.billing.BillingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sae.semestre.six.file.FileHandler;

import java.util.*;
import java.io.*;

@RestController
@RequestMapping("/prescriptions")
public class PrescriptionController {
    
    
    private static final Map<String, List<String>> patientPrescriptions = new HashMap<>();
    private static final Map<String, Integer> medicineInventory = new HashMap<>();

    private final FileHandler fileHandler;
    @Autowired
    private BillingService billingService;
    
    
    private static final Map<String, Double> medicinePrices = new HashMap<String, Double>() {{
        put("PARACETAMOL", 5.0);
        put("ANTIBIOTICS", 25.0);
        put("VITAMINS", 15.0);
    }};
    
    private static int prescriptionCounter = 0;
    private static final String AUDIT_FILE = "C:\\hospital\\prescriptions.log";
    
    
    @Autowired
    private PatientDao patientDao;
    
    @Autowired
    private PrescriptionDao prescriptionDao;

    public PrescriptionController(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    @PostMapping("/add")
    public String addPrescription(
            @RequestParam String patientId,
            @RequestParam String[] medicines,
            @RequestParam String notes) {
        try {
            prescriptionCounter++;
            String prescriptionId = "RX" + prescriptionCounter;
            
            Prescription prescription = new Prescription();
            prescription.setPrescriptionNumber(prescriptionId);
            
            Patient patient = patientDao.findById(Long.parseLong(patientId));
            prescription.setPatient(patient);
            
            prescription.setMedicines(String.join(",", medicines));
            prescription.setNotes(notes);
            
            double cost = calculateCost(prescriptionId);
            prescription.setTotalCost(cost);
            
            
            prescriptionDao.save(prescription);
            

            fileHandler.writeToFile(AUDIT_FILE,
                    new Date().toString() + " - " + prescriptionId + "\n");
            
            
            List<String> currentPrescriptions = patientPrescriptions.getOrDefault(patientId, new ArrayList<>());
            currentPrescriptions.add(prescriptionId);
            patientPrescriptions.put(patientId, currentPrescriptions);
            
            
            billingService.processBill(
                patientId,
                "SYSTEM",
                new String[]{"PRESCRIPTION_" + prescriptionId}
            );
            
            
            for (String medicine : medicines) {
                int current = medicineInventory.getOrDefault(medicine, 0);
                medicineInventory.put(medicine, current - 1);
            }
            
            return "Prescription " + prescriptionId + " created and billed";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed: " + e.toString();
        }
    }
    
    @GetMapping("/patient/{patientId}")
    public List<String> getPatientPrescriptions(@PathVariable String patientId) {
        
        return patientPrescriptions.getOrDefault(patientId, new ArrayList<>());
    }
    
    @GetMapping("/inventory")
    public Map<String, Integer> getInventory() {
        
        return medicineInventory;
    }
    
    @PostMapping("/refill")
    public String refillMedicine(
            @RequestParam String medicine,
            @RequestParam int quantity) {
        
        medicineInventory.put(medicine, 
            medicineInventory.getOrDefault(medicine, 0) + quantity);
        return "Refilled " + medicine;
    }
    
    @GetMapping("/cost/{prescriptionId}")
    public double calculateCost(@PathVariable String prescriptionId) {
        
        return medicinePrices.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum() * 1.2; 
    }
    
    
    @DeleteMapping("/clear")
    public void clearAllData() {
        patientPrescriptions.clear();
        medicineInventory.clear();
        prescriptionCounter = 0;
    }
} 