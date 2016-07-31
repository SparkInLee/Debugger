package com.lee.gui;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JScrollPane;

public class DebugApp implements CallBack<String> {

    private JFrame frame;

    private JTextField packageText;
    private String packageName;

    private JTextField hostText;
    private String host;
    private String ip;
    private int port;

    private JTextField rootDirText;
    private String rootDir;

    private JTextField dbText;
    private String dbName;

    private JTextField spText;
    private String spName;

    private JTextField fileText;
    private String fileName;

    private JTextArea consoleArea;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    DebugApp window = new DebugApp();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public DebugApp() {
        readProperties();
        initialize();
    }

    private void readProperties() {
        File file = new File("gui/config.properties");
        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    String[] pair = line.split("=");
                    if (null != pair && pair.length == 2) {
                        if (pair[0].equals("packageName")) {
                            packageName = pair[1];
                        } else if (pair[0].equals("host")) {
                            host = pair[1];
                        } else if (pair[0].equals("rootDir")) {
                            rootDir = pair[1];
                        } else if (pair[0].equals("defaultDbName")) {
                            dbName = pair[1];
                        } else if (pair[0].equals("defaultSpName")) {
                            spName = pair[1];
                        } else if (pair[0].equals("defaultFileName")) {
                            fileName = pair[1];
                        } else {
                            // System.out.println("unexcepted pair : " + line);
                        }
                    } else {
                        // System.out.println("unexcepted pair : " + line);
                    }
                }
            } catch (Exception e) {
                // no-op
            }
        } else {
            // System.out.println("no config file");
        }
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 573, 541);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);

        JLabel label_4 = new JLabel("设置应用包名：");
        label_4.setHorizontalAlignment(SwingConstants.CENTER);
        label_4.setBounds(27, 21, 95, 16);
        frame.getContentPane().add(label_4);

        packageText = new JTextField();
        if (null != packageName) {
            packageText.setText(packageName);
        }
        packageText.setToolTipText("");
        packageText.setColumns(10);
        packageText.setBounds(146, 15, 297, 28);
        packageText.getDocument().addDocumentListener(new DocumentListenerAdapter(new TextChangeListener() {

            @Override
            public void onTextChange(String content) {
                if (checkPackage()) {
                    NetUtils.getInstance().setPackageName(packageName);
                } else {
                    isValid = false;
                }
            }

        }));
        frame.getContentPane().add(packageText);

        JLabel lblNewLabel_1 = new JLabel("设置IP地址：");
        lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
        lblNewLabel_1.setBounds(27, 49, 95, 16);
        frame.getContentPane().add(lblNewLabel_1);
        hostText = new JTextField();
        hostText.setToolTipText("0.0.0.0（手机IP地址，必填）");
        hostText.setBounds(146, 43, 297, 28);
        if (null != host) {
            hostText.setText(host);
        }
        hostText.setColumns(10);
        hostText.getDocument().addDocumentListener(new DocumentListenerAdapter(new TextChangeListener() {

            @Override
            public void onTextChange(String content) {
                if (checkIP()) {
                    NetUtils.getInstance().setIp(host);
                } else {
                    isValid = false;
                }
            }

        }));
        frame.getContentPane().add(hostText);

        JLabel label_1 = new JLabel("存储根目录：");
        label_1.setHorizontalAlignment(SwingConstants.CENTER);
        label_1.setBounds(27, 78, 95, 16);
        frame.getContentPane().add(label_1);
        rootDirText = new JTextField();
        rootDirText.setToolTipText("默认为应用所在目录");
        if (null != rootDir) {
            rootDirText.setText(rootDir);
        }
        rootDirText.setColumns(10);
        rootDirText.setBounds(146, 72, 297, 28);
        rootDirText.getDocument().addDocumentListener(new DocumentListenerAdapter(new TextChangeListener() {

            @Override
            public void onTextChange(String content) {
                if (checkRootDir()) {
                    NetUtils.getInstance().setRootDir(rootDir);
                } else {
                    isValid = false;
                }
            }

        }));
        frame.getContentPane().add(rootDirText);

        JLabel label = new JLabel("拷贝数据库：");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBounds(27, 106, 95, 16);
        frame.getContentPane().add(label);
        dbText = new JTextField();
        if (null != dbName) {
            dbText.setText(dbName);
        }
        dbText.setColumns(10);
        dbText.setBounds(146, 100, 297, 28);
        frame.getContentPane().add(dbText);
        JButton copyDBBtn = new JButton("拷贝");
        copyDBBtn.setToolTipText("将数据库保存在{dbName}");
        copyDBBtn.setBounds(455, 106, 95, 29);
        copyDBBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                if (checkValid()) {
                    dbName = dbText.getText();
                    if (null != dbName && dbName.length() > 0) {
                        NetUtils.getInstance().copyDatabase(dbName, DebugApp.this);
                    }
                }
            }
        });
        frame.getContentPane().add(copyDBBtn);

        JLabel lblsp = new JLabel("拷贝SP文件：");
        lblsp.setHorizontalAlignment(SwingConstants.CENTER);
        lblsp.setBounds(27, 134, 95, 16);
        frame.getContentPane().add(lblsp);
        spText = new JTextField();
        if (null != spName) {
            spText.setText(spName);
        }
        spText.setColumns(10);
        spText.setBounds(146, 128, 297, 28);
        frame.getContentPane().add(spText);
        JButton copySPBtn = new JButton("拷贝");
        copySPBtn.setToolTipText("将SP文件保存在{spName}");
        copySPBtn.setBounds(455, 134, 95, 29);
        copySPBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                if (checkValid()) {
                    spName = spText.getText();
                    if (null != spName && spName.length() > 0) {
                        NetUtils.getInstance().copySharedPreference(spName, DebugApp.this);
                    }
                }
            }
        });
        frame.getContentPane().add(copySPBtn);

        JLabel label_3 = new JLabel("拷贝文件：");
        label_3.setHorizontalAlignment(SwingConstants.CENTER);
        label_3.setBounds(27, 162, 95, 16);
        frame.getContentPane().add(label_3);
        fileText = new JTextField();
        if (null != fileName) {
            fileText.setText(fileName);
        }
        fileText.setColumns(10);
        fileText.setBounds(146, 156, 297, 28);
        frame.getContentPane().add(fileText);

        JButton copyFileBtn = new JButton("拷贝");
        copyFileBtn.setToolTipText("将SP文件保存在{spName}");
        copyFileBtn.setBounds(455, 162, 95, 29);
        copyFileBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                if (checkValid()) {
                    fileName = fileText.getText();
                    if (null != fileName && fileName.length() > 0) {
                        NetUtils.getInstance().copyFile(fileName, DebugApp.this);
                    }
                }
            }
        });
        frame.getContentPane().add(copyFileBtn);

        JLabel label_2 = new JLabel("打印堆栈信息：");
        label_2.setHorizontalAlignment(SwingConstants.CENTER);
        label_2.setBounds(35, 195, 95, 16);
        frame.getContentPane().add(label_2);

        JButton dumpStackBtn = new JButton("打印堆栈");
        dumpStackBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (checkValid()) {
                    NetUtils.getInstance().dumpStackTrace(DebugApp.this);
                }
            }
        });
        dumpStackBtn.setToolTipText("将堆栈信息打印在日志窗口");
        dumpStackBtn.setBounds(144, 190, 95, 29);
        frame.getContentPane().add(dumpStackBtn);

        JButton saveStackBtn = new JButton("保存堆栈");
        saveStackBtn.setToolTipText("将堆栈信息保存在文件st.txt");
        saveStackBtn.setBounds(245, 190, 95, 29);
        saveStackBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                if (checkValid()) {
                    NetUtils.getInstance().saveDumpStackTrace(DebugApp.this);
                }
            }
        });
        frame.getContentPane().add(saveStackBtn);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(27, 231, 523, 240);
        frame.getContentPane().add(scrollPane);
        consoleArea = new JTextArea();
        scrollPane.setViewportView(consoleArea);
        consoleArea.setEditable(false);
        consoleArea.setLineWrap(true);

        JButton clearBtn = new JButton("清空输出");
        clearBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                consoleArea.setText("");
            }
        });
        clearBtn.setBounds(230, 474, 117, 29);
        frame.getContentPane().add(clearBtn);
    }

    private boolean isValid = false;

    private boolean checkValid() {
        if (isValid)
            return true;

        if (checkIP() && checkPackage() && checkRootDir()) {
            isValid = true;
            NetUtils.getInstance().init(packageName, ip, port, rootDir);
            return true;
        }

        return false;
    }

    private boolean checkPackage() {
        packageName = packageText.getText();
        if (null == packageName || packageName.length() == 0) {
            consoleArea.append("packageName is null or illegal.\n");
            return false;
        }
        return true;
    }

    private boolean checkIP() {
        host = hostText.getText();
        if (null == host || host.length() == 0) {
            consoleArea.append("host is null.\n");
            return false;
        }
        String[] hosts = host.split(":", 2);
        if (hosts.length == 2) {
            ip = hosts[0];
            String regex = "^(((\\d{1,2})|(1\\d{2})|(2[0-4]\\d)|(25[0-5]))\\.){3}((\\d{1,2})|(1\\d{2})|(2[0-4]\\d)|(25[0-5]))$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(ip);
            if (!matcher.find()) {
                consoleArea.append("ip is illegal.\n");
                return false;
            }

            try {
                port = Integer.parseInt(hosts[1]);
            } catch (NumberFormatException e) {
                consoleArea.append("port is illegal.\n");
                return false;
            }

            return true;
        } else {
            consoleArea.append("host is illegal.\n");
            return false;
        }
    }

    private boolean checkRootDir() {
        rootDir = rootDirText.getText();
        if (null == rootDir || rootDir.length() == 0) {
            rootDir = new File("out").getAbsolutePath();
            return true;
        }
        File file = new File(rootDir);
        if (file.exists() && file.isDirectory()) {
            return true;
        }
        consoleArea.append("rootDir is null or illegal.\n");
        return false;
    }

    @Override
    public void response(final String t) {
        // TODO Auto-generated method stub
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                consoleArea.append("success : ");
                consoleArea.append(t);
                consoleArea.append("\n");
                consoleArea.append("\n");
            }
        });
    }

    @Override
    public void error(final Exception e) {
        // TODO Auto-generated method stub
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                consoleArea.append("error : ");
                consoleArea.append(e.getClass().getSimpleName() + " : " + e.getMessage());
                consoleArea.append("\n");
                consoleArea.append("\n");
            }
        });
    }

    static class DocumentListenerAdapter implements DocumentListener {
        TextChangeListener listener;

        public DocumentListenerAdapter(TextChangeListener listener) {
            this.listener = listener;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            onTextChange(e.getDocument());
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            onTextChange(e.getDocument());
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            onTextChange(e.getDocument());
        }

        public void onTextChange(Document document) {
            try {
                if (null != listener) {
                    listener.onTextChange(document.getText(0, document.getLength()));
                }
            } catch (BadLocationException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

    }

    static interface TextChangeListener {
        void onTextChange(String content);
    }
}
