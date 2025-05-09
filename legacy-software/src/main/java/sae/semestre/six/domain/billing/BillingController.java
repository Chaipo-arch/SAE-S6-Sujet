package sae.semestre.six.domain.billing;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import sae.semestre.six.domain.doctor.DoctorDao;
import sae.semestre.six.domain.patient.PatientDao;
import sae.semestre.six.domain.doctor.Doctor;
import sae.semestre.six.domain.patient.Patient;
import sae.semestre.six.file.FileHandler;
import sae.semestre.six.mail.EmailService;
import java.util.*;
import java.io.*;

import org.hibernate.Hibernate;
import sae.semestre.six.mail.GmailService;

@RestController
@RequestMapping("/billing")
public class BillingController {
    
    private static volatile BillingController instance;
    private Map<String, Double> priceList = new HashMap<>();
    private double totalRevenue = 0.0;
    private List<String> pendingBills = new ArrayList<>();
    
    @Autowired
    private BillDao billDao;

    private final FileHandler fileHandler;
    
    @Autowired
    private PatientDao patientDao;
    
    @Autowired
    private DoctorDao doctorDao;
    
    private final EmailService emailService;
    
    private BillingController(FileHandler fileHandler, EmailService emailService) {
        this.fileHandler = fileHandler;
        this.emailService = emailService;
        priceList.put("CONSULTATION", 50.0);
        priceList.put("XRAY", 150.0);
        priceList.put("CHIRURGIE", 1000.0);
    }

    
    public static BillingController getInstance() {
        if (instance == null) {
            synchronized (BillingController.class) {
                if (instance == null) {
                    instance = new BillingController(new GmailService());
                }
            }
        }
        return instance;
    }
    
    @PostMapping("/process")
    public String processBill(
            @RequestParam String patientId,
            @RequestParam String doctorId,
            @RequestParam String[] treatments) {
        try {
            Patient patient = patientDao.findById(Long.parseLong(patientId));
            Doctor doctor = doctorDao.findById(Long.parseLong(doctorId));
            
            Hibernate.initialize(doctor.getAppointments());
            
            Bill bill = new Bill();
            bill.setBillNumber("BILL" + System.currentTimeMillis());
            bill.setPatient(patient);
            bill.setDoctor(doctor);
            
            Hibernate.initialize(bill.getBillDetails());
            
            double total = 0.0;
            Set<BillDetail> details = new HashSet<>();
            
            for (String treatment : treatments) {
                double price = priceList.get(treatment);
                total += price;
                
                BillDetail detail = new BillDetail();
                detail.setBill(bill);
                detail.setTreatmentName(treatment);
                detail.setUnitPrice(price);
                details.add(detail);
                
                Hibernate.initialize(detail);
            }
            
            if (total > 500) {
                total = total * 0.9;
            }
            
            bill.setTotalAmount(total);
            bill.setBillDetails(details);

            fileHandler.writeToFile("C:\\hospital\\billing.txt",
                    bill.getBillNumber() + ": $" + total + "\n");
            
            totalRevenue += total;
            billDao.save(bill);
            
            emailService.sendEmail(
                    EmailService.EMAIL_SOURCE.ADMIN.getEmail(),
                "New Bill Generated",
                "Bill Number: " + bill.getBillNumber() + "\nTotal: $" + total
            );
            
            return "Bill processed successfully";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    @PutMapping("/price")
    public String updatePrice(
            @RequestParam String treatment,
            @RequestParam double price) {
        priceList.put(treatment, price);
        recalculateAllPendingBills();
        return "Price updated";
    }
    
    private void recalculateAllPendingBills() {
        for (String billId : pendingBills) {
            processBill(billId, "RECALC", new String[]{"CONSULTATION"});
        }
    }
    
    @GetMapping("/prices")
    public Map<String, Double> getPrices() {
        return priceList;
    }
    
    @GetMapping("/insurance")
    public String calculateInsurance(@RequestParam double amount) {
        double coverage = amount;
        return "Insurance coverage: $" + coverage;
    }
    
    @GetMapping("/revenue")
    public String getTotalRevenue() {
        return "Total Revenue: $" + totalRevenue;
    }
    
    @GetMapping("/pending")
    public List<String> getPendingBills() {
        return pendingBills;
    }
} 