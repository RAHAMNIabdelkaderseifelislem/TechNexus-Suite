// src/main/java/com/yourstore/app/backend/config/DataInitializer.java
package com.yourstore.app.backend.config;

import com.yourstore.app.backend.model.entity.*; // Assuming all entities are here
import com.yourstore.app.backend.model.enums.ProductCategory;
import com.yourstore.app.backend.model.enums.RepairStatus;
import com.yourstore.app.backend.model.enums.UserRole;
import com.yourstore.app.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Recommended for complex initializations

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final RepairJobRepository repairJobRepository;
    // Assuming SaleItemRepository and PurchaseItemRepository are not directly needed
    // if cascade is set up correctly in Sale and Purchase entities.

    @Autowired
    public DataInitializer(UserRepository userRepository,
                           @Lazy PasswordEncoder passwordEncoder,
                           ProductRepository productRepository,
                           SaleRepository saleRepository,
                           PurchaseRepository purchaseRepository,
                           RepairJobRepository repairJobRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.purchaseRepository = purchaseRepository;
        this.repairJobRepository = repairJobRepository;
    }

    @Override
    @Transactional // Wrap the whole run method in a transaction for consistency
    public void run(String... args) throws Exception {
        User adminUser = null;
        User regularUser = null;

        if (!userRepository.existsByUsername("admin")) {
            adminUser = new User(
                    "admin",
                    passwordEncoder.encode("adminpass"),
                    Set.of(UserRole.ROLE_ADMIN, UserRole.ROLE_USER, UserRole.ROLE_MANAGER, UserRole.ROLE_STAFF) // Give admin all roles for ease
            );
            userRepository.save(adminUser);
            System.out.println("Created default admin user (admin/adminpass)");
        } else {
            adminUser = userRepository.findByUsername("admin").orElseThrow();
        }

        if (!userRepository.existsByUsername("user")) {
            regularUser = new User(
                    "user",
                    passwordEncoder.encode("userpass"),
                    Set.of(UserRole.ROLE_USER, UserRole.ROLE_STAFF) // Example: regular user can also be staff
            );
            userRepository.save(regularUser);
            System.out.println("Created default regular user (user/userpass)");
        } else {
            regularUser = userRepository.findByUsername("user").orElseThrow();
        }

        // Seed data only if no products exist (assuming this is the marker for a fresh DB)
        if (productRepository.count() == 0) {
            System.out.println("No products found, seeding initial data...");

            // --- 1. Create Products ---
            Product demoLaptop = new Product();
            demoLaptop.setName("Demo Laptop High-End"); // Renamed for clarity
            demoLaptop.setCategory(ProductCategory.LAPTOP);
            demoLaptop.setDescription("Un laptop de démonstration puissant avec les dernières spécifications.");
            demoLaptop.setPurchasePrice(new BigDecimal("70000.00"));
            demoLaptop.setSellingPrice(new BigDecimal("99990.00")); // Adjusted price
            demoLaptop.setQuantityInStock(8); // Start with a few
            demoLaptop.setSupplier("Fournisseur Global Tech");
            demoLaptop = productRepository.save(demoLaptop);

            Product demoMouse = new Product();
            demoMouse.setName("Souris Optique Standard"); // Renamed
            demoMouse.setCategory(ProductCategory.MOUSE);
            demoMouse.setDescription("Une souris optique basique pour usage quotidien.");
            demoMouse.setPurchasePrice(new BigDecimal("800.00"));
            demoMouse.setSellingPrice(new BigDecimal("1500.00")); // Adjusted price
            demoMouse.setQuantityInStock(40);
            demoMouse.setSupplier("Accessoires PC Alger");
            demoMouse = productRepository.save(demoMouse);

            Product desktopPC = new Product("PC de Bureau Performant", ProductCategory.DESKTOP_PC, "PC de bureau fiable pour usage quotidien et bureautique. Windows 11 préinstallé.", new BigDecimal("65000.00"), new BigDecimal("89990.00"), 12, "Alger PC Distribution");
            desktopPC = productRepository.save(desktopPC);

            Product monitor = new Product("Écran LED 24\" Full HD", ProductCategory.MONITOR, "Moniteur LED 24 pouces, résolution Full HD (1920x1080), HDMI & VGA.", new BigDecimal("18500.00"), new BigDecimal("24900.00"), 20, "ElectroTech Import");
            monitor = productRepository.save(monitor);

            Product keyboard = new Product("Clavier Mécanique Gamer RGB", ProductCategory.KEYBOARD, "Clavier mécanique rétroéclairé RGB, switchs bleus tactiles, AZERTY.", new BigDecimal("7500.00"), new BigDecimal("11500.00"), 25, "Accessoires Pro Algérie");
            keyboard = productRepository.save(keyboard);

            Product ssd = new Product("SSD NVMe 1To Rapide", ProductCategory.COMPONENT_STORAGE_SSD, "Disque SSD NVMe M.2 1To, vitesses de lecture/écriture élevées.", new BigDecimal("13000.00"), new BigDecimal("18500.00"), 18, "InfoStock DZ");
            ssd = productRepository.save(ssd);

            Product wirelessMouse = new Product("Souris Optique Sans Fil Ergonomique", ProductCategory.MOUSE, "Souris sans fil confortable, design ergonomique, 1600 DPI.", new BigDecimal("2200.00"), new BigDecimal("3500.00"), 30, "Solutions Informatiques Alger");
            wirelessMouse = productRepository.save(wirelessMouse);

            Product smartphone = new Product("Smartphone Android Milieu de Gamme", ProductCategory.OTHER, "Smartphone Android 12, 6.5\" HD+, 128Go stockage, 6Go RAM.", new BigDecimal("28000.00"), new BigDecimal("37900.00"), 15, "MobilePlus DZ");
            smartphone = productRepository.save(smartphone);

            Product tablet = new Product("Tablette Tactile 10 Pouces", ProductCategory.OTHER, "Tablette Android 10 pouces, idéale pour le divertissement et la navigation.", new BigDecimal("19500.00"), new BigDecimal("26500.00"), 10, "ElectroTech Import");
            tablet = productRepository.save(tablet);

            Product printer = new Product("Imprimante Multifonction Laser", ProductCategory.PRINTER, "Imprimante laser monochrome multifonction (Impression, Copie, Scan).", new BigDecimal("22000.00"), new BigDecimal("29900.00"), 8, "Bureau Moderne SARL");
            printer = productRepository.save(printer);
            System.out.println("Created initial set of products.");

            // --- 2. Create Sales ---
            Sale sale1 = new Sale();
            sale1.setCustomerName("Fatima Zohra Benali");
            sale1.setSaleDate(LocalDateTime.now().minusHours(5)); // A bit in the past
            sale1.setUser(regularUser); // user (ID 2 typically)
            SaleItem sale1Item1 = new SaleItem(sale1, demoLaptop, 1, demoLaptop.getSellingPrice());
            SaleItem sale1Item2 = new SaleItem(sale1, wirelessMouse, 1, wirelessMouse.getSellingPrice());
            sale1.setItems(new ArrayList<>(List.of(sale1Item1, sale1Item2)));
            sale1.calculateTotalAmount();
            saleRepository.save(sale1);
            demoLaptop.setQuantityInStock(demoLaptop.getQuantityInStock() - 1);
            wirelessMouse.setQuantityInStock(wirelessMouse.getQuantityInStock() - 1);
            productRepository.save(demoLaptop);
            productRepository.save(wirelessMouse);

            Sale sale2 = new Sale();
            sale2.setCustomerName("Ahmed Khaled");
            sale2.setSaleDate(LocalDateTime.now().minusDays(2).withHour(14).withMinute(30));
            sale2.setUser(adminUser); // admin (ID 1 typically)
            SaleItem sale2Item1 = new SaleItem(sale2, desktopPC, 1, desktopPC.getSellingPrice());
            SaleItem sale2Item2 = new SaleItem(sale2, monitor, 1, monitor.getSellingPrice());
            SaleItem sale2Item3 = new SaleItem(sale2, keyboard, 1, keyboard.getSellingPrice());
            sale2.setItems(new ArrayList<>(List.of(sale2Item1, sale2Item2, sale2Item3)));
            sale2.calculateTotalAmount();
            saleRepository.save(sale2);
            desktopPC.setQuantityInStock(desktopPC.getQuantityInStock() - 1);
            monitor.setQuantityInStock(monitor.getQuantityInStock() - 1);
            keyboard.setQuantityInStock(keyboard.getQuantityInStock() - 1);
            productRepository.save(desktopPC);
            productRepository.save(monitor);
            productRepository.save(keyboard);

            Sale sale3 = new Sale();
            // customerName is NULL for walk-in
            sale3.setSaleDate(LocalDateTime.now().minusDays(1).withHour(11).withMinute(0));
            sale3.setUser(regularUser);
            SaleItem sale3Item1 = new SaleItem(sale3, smartphone, 1, smartphone.getSellingPrice());
            sale3.setItems(new ArrayList<>(List.of(sale3Item1)));
            sale3.calculateTotalAmount();
            saleRepository.save(sale3);
            smartphone.setQuantityInStock(smartphone.getQuantityInStock() - 1);
            productRepository.save(smartphone);
            System.out.println("Created initial sales records.");

            // --- 3. Create Purchases ---
            Purchase purchase1 = new Purchase();
            purchase1.setSupplierName("InfoStock DZ");
            purchase1.setInvoiceNumber("FAC-ISDZ-2024-005");
            purchase1.setPurchaseDate(LocalDateTime.now().minusDays(7));
            purchase1.setUser(adminUser);
            PurchaseItem purchase1Item1 = new PurchaseItem(purchase1, demoLaptop, 5, new BigDecimal("62000.00"));
            PurchaseItem purchase1Item2 = new PurchaseItem(purchase1, ssd, 10, new BigDecimal("12500.00"));
            purchase1.setItems(new ArrayList<>(List.of(purchase1Item1, purchase1Item2)));
            purchase1.calculateTotalAmount();
            purchaseRepository.save(purchase1);
            demoLaptop.setQuantityInStock(demoLaptop.getQuantityInStock() + 5);
            // demoLaptop.setPurchasePrice(new BigDecimal("62000.00")); // Optionally update product cost
            ssd.setQuantityInStock(ssd.getQuantityInStock() + 10);
            // ssd.setPurchasePrice(new BigDecimal("12500.00"));
            productRepository.save(demoLaptop);
            productRepository.save(ssd);

            Purchase purchase2 = new Purchase();
            purchase2.setSupplierName("MobilePlus DZ");
            purchase2.setInvoiceNumber("MPDZ-BON-0231");
            purchase2.setPurchaseDate(LocalDateTime.now().minusDays(4));
            purchase2.setUser(adminUser);
            PurchaseItem purchase2Item1 = new PurchaseItem(purchase2, smartphone, 10, new BigDecimal("27000.00"));
            PurchaseItem purchase2Item2 = new PurchaseItem(purchase2, tablet, 5, new BigDecimal("18500.00"));
            purchase2.setItems(new ArrayList<>(List.of(purchase2Item1, purchase2Item2)));
            purchase2.calculateTotalAmount();
            purchaseRepository.save(purchase2);
            smartphone.setQuantityInStock(smartphone.getQuantityInStock() + 10);
            tablet.setQuantityInStock(tablet.getQuantityInStock() + 5);
            productRepository.save(smartphone);
            productRepository.save(tablet);
            System.out.println("Created initial purchase records.");

            // --- 4. Create Repair Jobs ---
            RepairJob repairJob1 = new RepairJob();
            repairJob1.setCustomerName("Demo Customer Repair");
            repairJob1.setCustomerPhone("0555123123");
            repairJob1.setItemType("Laptop");
            repairJob1.setItemBrand("DemoBrand");
            repairJob1.setItemModel("DemoModelX");
            repairJob1.setReportedIssue("Ne démarre pas.");
            repairJob1.setStatus(RepairStatus.READY_FOR_PICKUP); // Example from dashboard
            repairJob1.setEstimatedCost(new BigDecimal("5000.00"));
            repairJob1.setActualCost(new BigDecimal("4500.00"));
            repairJob1.setDateReceived(LocalDateTime.now().minusDays(10));
            repairJob1.setEstimatedCompletionDate(LocalDate.now().minusDays(5));
            repairJob1.setDateCompleted(LocalDateTime.now().minusDays(3));
            repairJob1.setAssignedToUser(adminUser);
            repairJob1.setTechnicianNotes("Remplacement du connecteur de charge et nettoyage interne. Testé OK.");
            repairJobRepository.save(repairJob1);

            RepairJob repairJob2 = new RepairJob();
            repairJob2.setCustomerName("Karim Mahmoudi");
            repairJob2.setCustomerPhone("0550123456");
            repairJob2.setCustomerEmail("karim.m@email.dz");
            repairJob2.setItemType("Laptop");
            repairJob2.setItemBrand("HP");
            repairJob2.setItemModel("ProBook 450 G8");
            repairJob2.setItemSerialNumber("HPPRO450G8XYZ01");
            repairJob2.setReportedIssue("L'ordinateur portable ne démarre plus, aucun voyant.");
            repairJob2.setTechnicianNotes("Diagnostic initial: possible problème carte mère ou alimentation interne.");
            repairJob2.setStatus(RepairStatus.PENDING_ASSESSMENT);
            repairJob2.setAssignedToUser(adminUser);
            repairJob2.setEstimatedCost(new BigDecimal("8000.00"));
            repairJob2.setDateReceived(LocalDateTime.now().minusDays(2));
            repairJob2.setEstimatedCompletionDate(LocalDate.now().plusDays(1)); // Est. 3 days from received
            repairJobRepository.save(repairJob2);

            RepairJob repairJob3 = new RepairJob();
            repairJob3.setCustomerName("Amina Haddad");
            repairJob3.setCustomerPhone("0771987654");
            repairJob3.setCustomerEmail("amina.h@email.com");
            repairJob3.setItemType("Smartphone");
            repairJob3.setItemBrand("Samsung");
            repairJob3.setItemModel("Galaxy A52");
            repairJob3.setItemSerialNumber("SMA52ALGQWERT");
            repairJob3.setReportedIssue("Écran fissuré après une chute. Tactile fonctionne partiellement.");
            repairJob3.setTechnicianNotes("Remplacement écran complet nécessaire. Pièce en commande.");
            repairJob3.setStatus(RepairStatus.WAITING_FOR_PARTS);
            // Not assigned yet
            repairJob3.setEstimatedCost(new BigDecimal("12000.00"));
            repairJob3.setDateReceived(LocalDateTime.now().minusDays(1));
            repairJob3.setEstimatedCompletionDate(LocalDate.now().plusDays(4)); // Est. 5 days from received
            repairJobRepository.save(repairJob3);

            RepairJob repairJob4 = new RepairJob();
            repairJob4.setCustomerName("Yacine Djebbar");
            repairJob4.setCustomerPhone("0662334455");
            repairJob4.setItemType("Desktop PC");
            repairJob4.setItemBrand("Assemblé (Custom)");
            repairJob4.setReportedIssue("Très lent au démarrage et pendant l'utilisation, publicités intempestives.");
            repairJob4.setTechnicianNotes("Scan antivirus complet effectué. Plusieurs malwares supprimés. Optimisation des programmes de démarrage. Nettoyage des fichiers temporaires.");
            repairJob4.setStatus(RepairStatus.COMPLETED_PAID);
            repairJob4.setAssignedToUser(adminUser);
            repairJob4.setEstimatedCost(new BigDecimal("3500.00"));
            repairJob4.setActualCost(new BigDecimal("3500.00"));
            repairJob4.setDateReceived(LocalDateTime.now().minusDays(4));
            repairJob4.setDateCompleted(LocalDateTime.now().minusDays(1));
            repairJobRepository.save(repairJob4);
            System.out.println("Created initial repair job records.");

            System.out.println("Default data seeding completed.");
        } else {
            System.out.println("Database already contains products. Skipping initial data seeding.");
        }
    }
}

// Helper constructor for Product if you don't have one (add to Product.java)
// public Product(String name, ProductCategory category, String description, BigDecimal purchasePrice, BigDecimal sellingPrice, int quantityInStock, String supplier) {
//     this.name = name;
//     this.category = category;
//     this.description = description;
//     this.purchasePrice = purchasePrice;
//     this.sellingPrice = sellingPrice;
//     this.quantityInStock = quantityInStock;
//     this.supplier = supplier;
// }