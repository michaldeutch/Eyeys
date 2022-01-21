package notTested;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
import com.opencsv.CSVWriter;


import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Processor {
    private Date limit;
    private Map<String, Parent> allMothers = new HashMap<>();
    private Map<String, Parent> allFathers = new HashMap<>();
    private Map<String, Date> lastVisited = new HashMap<>();
    private Set<String> notTested = new HashSet<>();
    private String allFile;
    private String notTestedFile;

    public void populateAllParents() throws Exception {
        AtomicReference<Parent> lastMother = new AtomicReference<>();
        AtomicReference<Parent> lastFather = new AtomicReference<>();
        getAllPatients().forEach(arr -> {
            try {
                if (arr.length > 1 && !arr[Utils.KID_ID_COL].isEmpty()) {
                    Date lastVisit=new SimpleDateFormat("dd/MM/yy").parse(arr[Utils.LAST_VISIT_COL]);
                    lastVisited.put(arr[Utils.KID_ID_COL], lastVisit);
                    if (arr[Utils.MOTHER_PHONE_COL].isEmpty()) {
                        if (arr[Utils.MOM_NAME_COL].equals(lastMother.get().name)) {
                            if (notTested.contains(arr[Utils.KID_ID_COL]) && lastVisit.compareTo(limit) < 0)
                                lastMother.get().kids.add(arr[Utils.KID_NAME_COL]);
                            allMothers.put(arr[Utils.KID_ID_COL], lastMother.get());
                        }
                        else
                            System.out.println("Bad mom... " + Arrays.stream(arr).toList());
                    } else {
                        Parent mom = new Parent();
                        mom.name = arr[Utils.MOM_NAME_COL];
                        if (notTested.contains(arr[Utils.KID_ID_COL]) && lastVisit.compareTo(limit) < 0)
                            mom.kids.add(arr[Utils.KID_NAME_COL]);
                        mom.phone = arr[Utils.MOTHER_PHONE_COL];
                        lastMother.set(mom);
                        allMothers.put(arr[Utils.KID_ID_COL], lastMother.get());
                    }

                    if (arr[Utils.DAD_PHONE_COL].isEmpty()) {
                        if (arr[Utils.DAD_NAME_COL].equals(lastFather.get().name)) {
                            if (notTested.contains(arr[Utils.KID_ID_COL]) && lastVisit.compareTo(limit) < 0)
                                lastFather.get().kids.add(arr[Utils.KID_NAME_COL]);
                            allFathers.put(arr[Utils.KID_ID_COL], lastFather.get());
                        }
                        else
                            System.out.println("Bad dad... " + Arrays.stream(arr).toList());
                    } else {
                        Parent dad = new Parent();
                        dad.name = arr[Utils.DAD_NAME_COL];
                        if (notTested.contains(arr[Utils.KID_ID_COL]) && lastVisit.compareTo(limit) < 0)
                            dad.kids.add(arr[Utils.KID_NAME_COL]);
                        dad.phone = arr[Utils.DAD_PHONE_COL];
                        lastFather.set(dad);
                        allFathers.put(arr[Utils.KID_ID_COL], lastFather.get());
                    }
                }
            } catch (Exception e) {
                System.out.println("Failed to process row=" + Arrays.toString(arr));
                throw new RuntimeException("Failed to process the all patients file!");
            }
        });
    }

    public Stream<String> getNotTested() throws URISyntaxException, IOException {
        Path bookPath = Paths.get(notTestedFile);
        return Files.lines(bookPath).map(line -> line.split(",")[0]);
    }

    public Stream<String[]> getAllPatients() throws Exception {
        Path bookPath = Paths.get(allFile);
        return Files.lines(bookPath).skip(1)
                .map(line -> line.split(","));
    }

    private String getKidsString(List<String> kids) {
        if (kids.isEmpty())
            return "";
        if (kids.size() == 1) {
            return kids.get(0);
        }
        if (kids.size() == 2) {
            return kids.get(0) + " ו" + kids.get(1);
        }
        return String.join(", ", kids.subList(1, kids.size() - 1)) + " ו" + kids.get(0);
    }

    public String chooseFile(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Excel & Csv file", "xls", "xlsx", "csv");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        throw new RuntimeException("Bad file chosen: " + chooser.getSelectedFile().getName());
    }

    private void getInput() throws Exception {
        String allFileInput = chooseFile("הכנס קובץ ראשי");
        File file = File.createTempFile("all", ".csv");
        allFile = file.getAbsolutePath();
        file.deleteOnExit();
        Workbook book = new Workbook(allFileInput);
        book.save(allFile, SaveFormat.AUTO);

        String notTestedInput = chooseFile("הכנס קובץ ראשי");
        File file2 = File.createTempFile("not", ".csv");
        notTestedFile = file2.getAbsolutePath();
        file2.deleteOnExit();
        Workbook book2 = new Workbook(notTestedInput);
        book2.save(notTestedFile, SaveFormat.AUTO);

        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String inputDate = JOptionPane.showInputDialog("הכנס תאריך לסינון מי שביקר אחריו", df.format(new Date()));
        limit = df.parse(inputDate);
    }

    public void process() throws Exception {
        getInput();
        notTested = getNotTested().collect(Collectors.toSet());
        populateAllParents();
        File file = File.createTempFile("Parents", ".csv");
        file.deleteOnExit();
        FileWriter outputfile = new FileWriter(file);
        CSVWriter writer = new CSVWriter(outputfile);
        String[] header = { "טלפון", "ילדים", "שם ההורה" };
        writer.writeNext(header);

        // add data to csv
        getNotTested().filter(id -> lastVisited.containsKey(id))
                .filter(id -> lastVisited.get(id).compareTo(limit) < 0)
                .map(id -> allMothers.get(id)).filter(Objects::nonNull).distinct().forEach(parent -> {
                    String[] data = new String[3];
                    data[0] = parent.phone;
                    data[1] = getKidsString(parent.kids);
                    data[2] = parent.name;
                    writer.writeNext(data);
                });

        getNotTested().filter(id -> lastVisited.containsKey(id))
                .filter(id -> lastVisited.get(id).compareTo(limit) < 0)
                .map(id -> allFathers.get(id)).filter(Objects::nonNull).distinct().forEach(parent -> {
                    String[] data = new String[3];
                    data[0] = parent.phone;
                    data[1] = getKidsString(parent.kids);
                    data[2] = parent.name;
                    writer.writeNext(data);
                });
        Workbook book = new Workbook(file.getAbsolutePath());
        book.save("Parents.xlsx", SaveFormat.AUTO);
        Desktop.getDesktop().open(new File("Parents.xlsx"));
        writer.close();
    }

    public static class Parent {
        public List<String> kids = new ArrayList<>();
        public String phone;
        public String name;

        @Override
        public String toString() {
            return "Parent{" +
                    "kids=" + kids +
                    ", phone='" + phone + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}
