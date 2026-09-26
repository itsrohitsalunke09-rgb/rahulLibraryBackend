package org.ngolibrary.config;

import java.time.LocalDate;
import java.util.List;

import org.ngolibrary.domain.Book;
import org.ngolibrary.domain.BookIssue;
import org.ngolibrary.domain.IssueStatus;
import org.ngolibrary.domain.Role;
import org.ngolibrary.domain.UserAccount;
import org.ngolibrary.repo.BookIssueRepository;
import org.ngolibrary.repo.BookRepository;
import org.ngolibrary.repo.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(UserAccountRepository users, BookRepository books,
                               BookIssueRepository issues, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }

            UserAccount admin = user("admin", "admin123", "Rohit Admin", "admin@ngolibrary.org",
                    "9000000001", Role.ADMIN, encoder);
            UserAccount librarian = user("librarian", "lib123", "Meera Librarian", "librarian@ngolibrary.org",
                    "9000000002", Role.LIBRARIAN, encoder);
            UserAccount student = user("student", "student123", "Aarav Student", "student@ngolibrary.org",
                    "9000000003", Role.STUDENT, encoder);
            UserAccount student2 = user("priya", "student123", "Priya Sharma", "priya@ngolibrary.org",
                    "9000000004", Role.STUDENT, encoder);
            users.saveAll(List.of(admin, librarian, student, student2));

            Book ncert = book("NCERT Science Class 10", "NCERT", "REG-001", "Science",
                    "Core science textbook for secondary students.", "NCERT", 2020, 300, "NCERT Books", "RCPT-001", 250.0, 8);
            Book maths = book("Mathematics for Class 8", "R.S. Aggarwal", "REG-002", "Mathematics",
                    "Practice-heavy maths workbook.", "Bharati Bhawan", 2019, 450, "Aggarwal Books", "RCPT-002", 320.0, 5);
            Book stories = book("Panchatantra Stories", "Vishnu Sharma", "REG-003", "Literature",
                    "Classic fables for young readers.", "Children's Book Trust", 2018, 200, "Story Publishers", "RCPT-003", 150.0, 6);
            Book civics = book("Indian Constitution at Work", "NCERT", "REG-004", "Civics",
                    "Introduction to the Indian Constitution.", "NCERT", 2021, 180, "NCERT Books", "RCPT-004", 120.0, 4);
            Book english = book("Wings of Fire", "A.P.J. Abdul Kalam", "REG-005", "Biography",
                    "Autobiography of Dr. Kalam.", "Universities Press", 2020, 220, "Biography House", "RCPT-005", 280.0, 3);
            Book geography = book("Geography: India Physical Environment", "NCERT", "REG-006", "Geography",
                    "Physical geography of India for Class 11.", "NCERT", 2021, 250, "NCERT Books", "RCPT-006", 200.0, 5);
            Book history = book("Themes in World History", "NCERT", "REG-007", "History",
                    "World history themes for Class 11.", "NCERT", 2020, 280, "NCERT Books", "RCPT-007", 180.0, 4);
            Book economics = book("Indian Economic Development", "NCERT", "REG-008", "Economics",
                    "Indian economy since independence.", "NCERT", 2022, 260, "NCERT Books", "RCPT-008", 220.0, 6);
            Book physics1 = book("Physics Part 1 Class 12", "NCERT", "REG-009", "Physics",
                    "Electrostatics, current electricity, magnetism.", "NCERT", 2021, 320, "NCERT Books", "RCPT-009", 240.0, 7);
            Book physics2 = book("Physics Part 2 Class 12", "NCERT", "REG-010", "Physics",
                    "Optics, dual nature, atoms, nuclei.", "NCERT", 2021, 300, "NCERT Books", "RCPT-010", 230.0, 7);
            Book chemistry1 = book("Chemistry Part 1 Class 12", "NCERT", "REG-011", "Chemistry",
                    "Solid state, solutions, electrochemistry.", "NCERT", 2021, 310, "NCERT Books", "RCPT-011", 250.0, 6);
            Book chemistry2 = book("Chemistry Part 2 Class 12", "NCERT", "REG-012", "Chemistry",
                    "Chemical kinetics, surface chemistry, p-block.", "NCERT", 2021, 300, "NCERT Books", "RCPT-012", 240.0, 6);
            Book biology1 = book("Biology Class 12", "NCERT", "REG-013", "Biology",
                    "Reproduction, genetics, evolution, ecology.", "NCERT", 2021, 350, "NCERT Books", "RCPT-013", 280.0, 8);
            Book maths12 = book("Mathematics Part 1 Class 12", "NCERT", "REG-014", "Mathematics",
                    "Relations, functions, calculus, algebra.", "NCERT", 2021, 280, "NCERT Books", "RCPT-014", 220.0, 7);
            Book maths12_2 = book("Mathematics Part 2 Class 12", "NCERT", "REG-015", "Mathematics",
                    "Integrals, differential equations, vectors.", "NCERT", 2021, 280, "NCERT Books", "RCPT-015", 210.0, 7);
            Book accountancy1 = book("Accountancy Part 1 Class 12", "NCERT", "REG-016", "Commerce",
                    "Partnership accounts, company accounts.", "NCERT", 2021, 290, "NCERT Books", "RCPT-016", 200.0, 5);
            Book accountancy2 = book("Accountancy Part 2 Class 12", "NCERT", "REG-017", "Commerce",
                    "Financial statements, cash flow.", "NCERT", 2021, 270, "NCERT Books", "RCPT-017", 190.0, 5);
            Book business = book("Business Studies Class 12", "NCERT", "REG-018", "Commerce",
                    "Management, marketing, finance.", "NCERT", 2021, 260, "NCERT Books", "RCPT-018", 180.0, 6);
            Book cs = book("Computer Science with Python Class 12", "Sumita Arora", "REG-019", "Computer Science",
                    "Python, data structures, SQL, networks.", "Dhanpat Rai", 2022, 400, "CS Publishers", "RCPT-019", 350.0, 4);
            Book english1 = book("Flamingo English Class 12", "NCERT", "REG-020", "English",
                    "Prose and poetry reader.", "NCERT", 2021, 220, "NCERT Books", "RCPT-020", 150.0, 8);
            Book english2 = book("Vistas English Class 12", "NCERT", "REG-021", "English",
                    "Supplementary reader.", "NCERT", 2021, 200, "NCERT Books", "RCPT-021", 140.0, 8);
            Book hindi1 = book("Aroh Hindi Class 12", "NCERT", "REG-022", "Hindi",
                    "Kavya khand - poetry section.", "NCERT", 2021, 240, "NCERT Books", "RCPT-022", 160.0, 6);
            Book hindi2 = book("Vitan Hindi Class 12", "NCERT", "REG-023", "Hindi",
                    "Gadya khand - prose section.", "NCERT", 2021, 220, "NCERT Books", "RCPT-023", 150.0, 6);
            Book polsci = book("Political Science Class 12", "NCERT", "REG-024", "Political Science",
                    "Contemporary world politics.", "NCERT", 2021, 250, "NCERT Books", "RCPT-024", 170.0, 5);
            Book sociology = book("Indian Society Class 12", "NCERT", "REG-025", "Sociology",
                    "Social institutions, change, challenges.", "NCERT", 2021, 230, "NCERT Books", "RCPT-025", 160.0, 4);

            // Marathi Literature
            Book mrutyunjay = book("Mrityunjay", "Shivaji Sawant", "REG-026", "Marathi Literature",
                    "Epic retelling of Karna's life from Mahabharata.", "Mehta Publishing", 1967, 600, "Mehta Publishing", "RCPT-026", 450.0, 5);
            Book yayati = book("Yayati", "V.S. Khandekar", "REG-027", "Marathi Literature",
                    "Classic novel about King Yayati's curse and redemption.", "Continental Prakashan", 1959, 320, "Continental Prakashan", "RCPT-027", 350.0, 4);
            Book kosala = book("Kosala", "Bhalchandra Nemade", "REG-028", "Marathi Literature",
                    "Modernist novel about a young man's existential crisis.", "Popular Prakashan", 1963, 280, "Popular Prakashan", "RCPT-028", 300.0, 4);
            Book shyamchi_aai = book("Shyamchi Aai", "Sane Guruji", "REG-029", "Marathi Literature",
                    "Autobiographical tribute to mother's love and values.", "Sane Guruji Smarak", 1935, 200, "Sane Guruji Smarak", "RCPT-029", 200.0, 6);
            Book batatyachi_chal = book("Batatyachi Chal", "P.L. Deshpande", "REG-030", "Marathi Literature",
                    "Humorous portrayal of a chawl community in Mumbai.", "Mauj Prakashan", 1971, 250, "Mauj Prakashan", "RCPT-030", 280.0, 5);
            Book ashtavakra = book("Ashtavakra", "Kusumagraj", "REG-031", "Marathi Literature",
                    "Poetic retelling of Ashtavakra's wisdom from Mahabharata.", "Rajhans Prakashan", 1978, 180, "Rajhans Prakashan", "RCPT-031", 250.0, 4);
            Book rakta_ganesha = book("Rakta Ganesha", "Narayan Dharap", "REG-032", "Marathi Literature",
                    "Thriller novel with mystery and suspense.", "Continental Prakashan", 1985, 220, "Continental Prakashan", "RCPT-032", 220.0, 3);
            Book panipat = book("Panipat", "Vishwas Patil", "REG-033", "Marathi Literature",
                    "Historical novel about the Third Battle of Panipat.", "Mehta Publishing", 1988, 550, "Mehta Publishing", "RCPT-033", 420.0, 4);
            Book zolya = book("Zolya", "P.L. Deshpande", "REG-034", "Marathi Literature",
                    "Collection of humorous essays and character sketches.", "Mauj Prakashan", 1974, 200, "Mauj Prakashan", "RCPT-034", 240.0, 5);
            Book swami = book("Swami", "Ranjeet Desai", "REG-035", "Marathi Literature",
                    "Biographical novel about Madhavrao Peshwa.", "Continental Prakashan", 1965, 400, "Continental Prakashan", "RCPT-035", 380.0, 4);

            books.saveAll(List.of(ncert, maths, stories, civics, english,
                    geography, history, economics, physics1, physics2,
                    chemistry1, chemistry2, biology1, maths12, maths12_2,
                    accountancy1, accountancy2, business, cs, english1, english2,
                    hindi1, hindi2, polsci, sociology,
                    mrutyunjay, yayati, kosala, shyamchi_aai, batatyachi_chal,
                    ashtavakra, rakta_ganesha, panipat, zolya, swami));

            BookIssue open = new BookIssue();
            open.setBook(ncert);
            open.setStudent(student);
            open.setIssuedBy(librarian);
            open.setIssueDate(LocalDate.now().minusDays(10));
            open.setDueDate(LocalDate.now().plusDays(4));
            open.setStatus(IssueStatus.ISSUED);
            ncert.setAvailableCopies(ncert.getAvailableCopies() - 1);

            BookIssue overdue = new BookIssue();
            overdue.setBook(maths);
            overdue.setStudent(student2);
            overdue.setIssuedBy(librarian);
            overdue.setIssueDate(LocalDate.now().minusDays(20));
            overdue.setDueDate(LocalDate.now().minusDays(6));
            overdue.setStatus(IssueStatus.ISSUED);
            maths.setAvailableCopies(maths.getAvailableCopies() - 1);

            books.saveAll(List.of(ncert, maths));
            issues.saveAll(List.of(open, overdue));
        };
    }

    private UserAccount user(String username, String password, String name, String email,
                             String phone, Role role, PasswordEncoder encoder) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private Book book(String title, String author, String registrationNumber, String category, String description,
                       String publication, Integer publicationYear, Integer pageCount, String seller,
                       String receiptNumber, Double price, int copies) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setRegistrationNumber(registrationNumber);
        book.setCategory(category);
        book.setDescription(description);
        book.setPublication(publication);
        book.setPublicationYear(publicationYear);
        book.setPageCount(pageCount);
        book.setSeller(seller);
        book.setReceiptNumber(receiptNumber);
        book.setPrice(price);
        book.setTotalCopies(copies);
        book.setAvailableCopies(copies);
        return book;
    }
}
