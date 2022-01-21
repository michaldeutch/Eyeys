import notTested.Processor;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

public class Main {

    public static void main(String[] args) throws URISyntaxException, IOException, ParseException {
        try {
            Processor processor = new Processor();
            processor.process();
        } catch (Exception e) {
            System.out.println("got exception" + e);
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error :(" , JOptionPane.INFORMATION_MESSAGE);
        }
    }


}
