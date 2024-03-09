import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiPingTool extends JFrame {
    private JTextField[] ipFields;
    private JButton[] startButtons;
    private JButton[] stopButtons;
    private JTextArea[] pingResults;
    private JTextField intervalField;
    private JTextField countField;
    private JComboBox<Integer> terminalCountComboBox;

    private ExecutorService executorService;

    public MultiPingTool() {
        setTitle("Multi Ping Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new GridLayout(0, 1));

        // Initialize arrays
        ipFields = new JTextField[4];
        startButtons = new JButton[4];
        stopButtons = new JButton[4];
        pingResults = new JTextArea[4];

        // Initialize settings UI
        JPanel settingsPanel = new JPanel(new FlowLayout());
        settingsPanel.add(new JLabel("Interval (ms):"));
        intervalField = new JTextField("500", 5);
        settingsPanel.add(intervalField);
        settingsPanel.add(new JLabel("Count:"));
        countField = new JTextField(5);
        settingsPanel.add(countField);
        settingsPanel.add(new JLabel("Number of Terminals:"));
        Integer[] terminalCounts = {1, 2, 3, 4};
        terminalCountComboBox = new JComboBox<>(terminalCounts);
        terminalCountComboBox.addActionListener(new TerminalCountChangeListener());
        settingsPanel.add(terminalCountComboBox);
        add(settingsPanel);

        // Initialize ping terminals
        int initialTerminalCount = (int) terminalCountComboBox.getSelectedItem();
        for (int i = 0; i < initialTerminalCount; i++) {
            addTerminal(i);
        }

        executorService = Executors.newFixedThreadPool(initialTerminalCount);

        setVisible(true);
    }

    private void addTerminal(int index) {
        JPanel panel = new JPanel(new BorderLayout());
        ipFields[index] = new JTextField(15);
        panel.add(ipFields[index], BorderLayout.WEST);
        startButtons[index] = new JButton("Start");
        startButtons[index].addActionListener(new StartButtonListener(index));
        panel.add(startButtons[index], BorderLayout.CENTER);
        stopButtons[index] = new JButton("Stop");
        stopButtons[index].setEnabled(false);
        stopButtons[index].addActionListener(new StopButtonListener(index));
        panel.add(stopButtons[index], BorderLayout.EAST);
        pingResults[index] = new JTextArea(5, 20);
        pingResults[index].setEditable(false);
        JScrollPane scrollPane = new JScrollPane(pingResults[index]);
        panel.add(scrollPane, BorderLayout.SOUTH);
        add(panel);
    }

    private void applySettings() {
        try {
            int interval = intervalField.getText().isEmpty() ? 500 : Integer.parseInt(intervalField.getText());
            int count = countField.getText().isEmpty() ? -1 : Integer.parseInt(countField.getText());
            int terminalCount = (int) terminalCountComboBox.getSelectedItem();
            for (int i = 0; i < terminalCount; i++) {
                executorService.submit(new PingTask(ipFields[i].getText(), interval, count, i));
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid interval or count.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class TerminalCountChangeListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int terminalCount = (int) terminalCountComboBox.getSelectedItem();
            if (terminalCount > ipFields.length) {
                for (int i = ipFields.length; i < terminalCount; i++) {
                    addTerminal(i);
                }
            } else if (terminalCount < ipFields.length) {
                for (int i = terminalCount; i < ipFields.length; i++) {
                    removeTerminal(i);
                }
            }
            revalidate();
            repaint();
        }
    }

    private void removeTerminal(int index) {
        // Stop any running task in the terminal
        stopButtons[index].doClick();
        // Remove components from UI
        Container parent = getContentPane();
        parent.remove(ipFields[index]);
        parent.remove(startButtons[index]);
        parent.remove(stopButtons[index]);
        parent.remove(pingResults[index].getParent());
        // Shift components in arrays
        for (int i = index; i < ipFields.length - 1; i++) {
            ipFields[i] = ipFields[i + 1];
            startButtons[i] = startButtons[i + 1];
            stopButtons[i] = stopButtons[i + 1];
            pingResults[i] = pingResults[i + 1];
        }
        // Nullify the last component
        ipFields[ipFields.length - 1] = null;
        startButtons[startButtons.length - 1] = null;
        stopButtons[stopButtons.length - 1] = null;
        pingResults[pingResults.length - 1] = null;
    }

    private class StartButtonListener implements ActionListener {
        private int index;

        public StartButtonListener(int index) {
            this.index = index;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            startButtons[index].setEnabled(false);
            stopButtons[index].setEnabled(true);
            executorService.submit(new PingTask(ipFields[index].getText(), Integer.parseInt(intervalField.getText()), Integer.parseInt(countField.getText()), index));
        }
    }

    private class StopButtonListener implements ActionListener {
        private int index;

        public StopButtonListener(int index) {
            this.index = index;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            startButtons[index].setEnabled(true);
            stopButtons[index].setEnabled(false);
            // TODO: Implement stopping ping task
        }
    }

    private class PingTask implements Runnable {
        private String ip;
        private int interval;
        private int count;
        private int index;

        public PingTask(String ip, int interval, int count, int index) {
            this.ip = ip;
            this.interval = interval;
            this.count = count;
            this.index = index;
        }

        @Override
        public void run() {
            try {
                InetAddress address = InetAddress.getByName(ip);
                int successCount = 0;
                int failureCount = 0;
                long totalResponseTime = 0;
                long maxResponseTime = Long.MIN_VALUE;
                long minResponseTime = Long.MAX_VALUE;
                for (int i = 0; count == -1 || i < count; i++) {
                    long startTime = System.currentTimeMillis();
                    boolean reachable = address.isReachable(interval);
                    long responseTime = System.currentTimeMillis() - startTime;
                    if (reachable) {
                        successCount++;
                        totalResponseTime += responseTime;
                        maxResponseTime = Math.max(maxResponseTime, responseTime);
                        minResponseTime = Math.min(minResponseTime, responseTime);
                    } else {
                        failureCount++;
                    }
                    final String result = String.format("Ping %s: %s, Response Time: %d ms", ip, reachable ? "Success" : "Failure", responseTime);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            pingResults[index].append(result + "\n");
                        }
                    });
                    Thread.sleep(interval);
                }
                final String statistics = String.format("Statistics for %s: Successes: %d, Failures: %d, Average Response Time: %.2f ms, Max Response Time: %d ms, Min Response Time: %d ms",
                        ip, successCount, failureCount, (double) totalResponseTime / successCount, maxResponseTime, minResponseTime);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pingResults[index].append(statistics + "\n");
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MultiPingTool::new);
    }
}
