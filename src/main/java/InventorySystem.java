import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class InventorySystem extends JFrame {
    
    // Configurações do banco de dados
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/inventory";
    private static final String DB_USER = "akin";
    private static final String DB_PASSWORD = "NeaOnnim@1997";
    
    private Connection connection;
    private User currentUser;
    
    // Componentes para referência posterior
    private DefaultListModel<String> userListModel = new DefaultListModel<>();
    private DefaultListModel<String> sectorListModel = new DefaultListModel<>();
    private DefaultListModel<String> itemListModel = new DefaultListModel<>();
    private DefaultListModel<String> inventoryListModel = new DefaultListModel<>();
    
    public InventorySystem() {
        try {
            // Conectar ao banco de dados
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            createTables();
            createAdminUser();
            
            // Configurar a interface
            setTitle("EasyInvent - Sistema de Inventário");
            setSize(800, 600);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            
            showLoginScreen();
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar ao banco de dados: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Tabela de usuários
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "id SERIAL PRIMARY KEY, " +
                         "name VARCHAR(100) NOT NULL, " +
                         "sector VARCHAR(100), " +
                         "email VARCHAR(100) UNIQUE NOT NULL, " +
                         "password VARCHAR(100) NOT NULL, " +
                         "is_admin BOOLEAN DEFAULT false)");
            
            // Tabela de setores
            stmt.execute("CREATE TABLE IF NOT EXISTS sectors (" +
                         "id SERIAL PRIMARY KEY, " +
                         "name VARCHAR(100) UNIQUE NOT NULL)");
            
            // Tabela de itens
            stmt.execute("CREATE TABLE IF NOT EXISTS items (" +
                         "id SERIAL PRIMARY KEY, " +
                         "name VARCHAR(100) NOT NULL, " +
                         "value DECIMAL(10,2) NOT NULL, " +
                         "description TEXT, " +
                         "sector_id INTEGER REFERENCES sectors(id), " +
                         "user_id INTEGER REFERENCES users(id))");
            
            // Tabela de inventários
            stmt.execute("CREATE TABLE IF NOT EXISTS inventories (" +
                         "id SERIAL PRIMARY KEY, " +
                         "sector_id INTEGER REFERENCES sectors(id), " +
                         "user_id INTEGER REFERENCES users(id), " +
                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            
            // Tabela de relação inventário-itens
            stmt.execute("CREATE TABLE IF NOT EXISTS inventory_items (" +
                         "inventory_id INTEGER REFERENCES inventories(id), " +
                         "item_id INTEGER REFERENCES items(id), " +
                         "PRIMARY KEY (inventory_id, item_id))");
        }
    }
    
    private void createAdminUser() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO users (name, email, password, is_admin) " +
                "VALUES (?, ?, ?, true) " +
                "ON CONFLICT (email) DO NOTHING")) {
            
            stmt.setString(1, "Administrador");
            stmt.setString(2, "admin@inventario.com");
            stmt.setString(3, "admin123");
            stmt.executeUpdate();
        }
    }
    
    private void showLoginScreen() {
        getContentPane().removeAll();
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel lblTitle = new JLabel("EasyInvent - Sistema de Inventário", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        
        JLabel lblEmail = new JLabel("Email:");
        JTextField txtEmail = new JTextField(20);
        JLabel lblPassword = new JLabel("Senha:");
        JPasswordField txtPassword = new JPasswordField(20);
        JButton btnLogin = new JButton("Login");
        JButton btnExit = new JButton("Sair");
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(lblTitle, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(lblEmail, gbc);
        
        gbc.gridx = 1;
        panel.add(txtEmail, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(lblPassword, gbc);
        
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(btnLogin, gbc);
        
        gbc.gridy = 4;
        panel.add(btnExit, gbc);
        
        btnLogin.addActionListener(e -> {
            String email = txtEmail.getText();
            String password = new String(txtPassword.getPassword());
            
            try {
                currentUser = authenticateUser(email, password);
                if (currentUser != null) {
                    if (currentUser.isAdmin) {
                        showAdminDashboard();
                    } else {
                        showUserDashboard();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Credenciais inválidas!");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Erro de banco de dados: " + ex.getMessage());
            }
        });
        
        btnExit.addActionListener(e -> System.exit(0));
        
        add(panel);
        revalidate();
        repaint();
    }
    
    private User authenticateUser(String email, String password) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, name, sector, is_admin FROM users WHERE email = ? AND password = ?")) {
            
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("sector"),
                    email,
                    rs.getBoolean("is_admin")
                );
            }
        }
        return null;
    }
    
    private void showAdminDashboard() {
        getContentPane().removeAll();
        setLayout(new BorderLayout());
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Aba de Usuários
        JPanel userPanel = new JPanel(new BorderLayout());
        JPanel userButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddUser = new JButton("Adicionar");
        JButton btnEditUser = new JButton("Editar");
        JButton btnDeleteUser = new JButton("Excluir");
        JList<String> userList = new JList<>(userListModel);
        
        btnAddUser.addActionListener(e -> showAddUserDialog());
        btnEditUser.addActionListener(e -> {
            int index = userList.getSelectedIndex();
            if (index != -1) {
                String selected = userListModel.get(index);
                showEditUserDialog(selected);
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um usuário!");
            }
        });
        btnDeleteUser.addActionListener(e -> {
            int index = userList.getSelectedIndex();
            if (index != -1) {
                String selected = userListModel.get(index);
                deleteUser(selected);
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um usuário!");
            }
        });
        
        userButtonPanel.add(btnAddUser);
        userButtonPanel.add(btnEditUser);
        userButtonPanel.add(btnDeleteUser);
        
        userPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        userPanel.add(userButtonPanel, BorderLayout.SOUTH);
        
        // Aba de Setores
        JPanel sectorPanel = new JPanel(new BorderLayout());
        JPanel sectorButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddSector = new JButton("Adicionar");
        JButton btnEditSector = new JButton("Editar");
        JButton btnDeleteSector = new JButton("Excluir");
        JList<String> sectorList = new JList<>(sectorListModel);
        
        btnAddSector.addActionListener(e -> showAddSectorDialog());
        btnEditSector.addActionListener(e -> {
            int index = sectorList.getSelectedIndex();
            if (index != -1) {
                String selected = sectorListModel.get(index);
                showEditSectorDialog(selected);
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um setor!");
            }
        });
        btnDeleteSector.addActionListener(e -> {
            int index = sectorList.getSelectedIndex();
            if (index != -1) {
                String selected = sectorListModel.get(index);
                deleteSector(selected);
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um setor!");
            }
        });
        
        sectorButtonPanel.add(btnAddSector);
        sectorButtonPanel.add(btnEditSector);
        sectorButtonPanel.add(btnDeleteSector);
        
        sectorPanel.add(new JScrollPane(sectorList), BorderLayout.CENTER);
        sectorPanel.add(sectorButtonPanel, BorderLayout.SOUTH);
        
        // Aba de Itens
        JPanel itemPanel = new JPanel(new BorderLayout());
        JPanel itemButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddItem = new JButton("Adicionar");
        JButton btnEditItem = new JButton("Editar");
        JButton btnDeleteItem = new JButton("Excluir");
        JList<String> itemList = new JList<>(itemListModel);
        
        btnAddItem.addActionListener(e -> showAddItemDialog(null));
        btnEditItem.addActionListener(e -> {
            int index = itemList.getSelectedIndex();
            if (index != -1) {
                String selected = itemListModel.get(index);
                showEditItemDialog(selected);
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um item!");
            }
        });
        btnDeleteItem.addActionListener(e -> {
            int index = itemList.getSelectedIndex();
            if (index != -1) {
                String selected = itemListModel.get(index);
                deleteItem(selected);
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um item!");
            }
        });
        
        itemButtonPanel.add(btnAddItem);
        itemButtonPanel.add(btnEditItem);
        itemButtonPanel.add(btnDeleteItem);
        
        itemPanel.add(new JScrollPane(itemList), BorderLayout.CENTER);
        itemPanel.add(itemButtonPanel, BorderLayout.SOUTH);
        
        // Aba de Inventários
        JPanel inventoryPanel = new JPanel(new BorderLayout());
        JPanel inventoryButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnCreateInventory = new JButton("Criar");
        JButton btnDeleteInventory = new JButton("Excluir");
        JButton btnPrintReport = new JButton("Imprimir Relatório");
        JList<String> inventoryList = new JList<>(inventoryListModel);
        
        btnCreateInventory.addActionListener(e -> showCreateInventoryDialog());
        btnDeleteInventory.addActionListener(e -> {
            int index = inventoryList.getSelectedIndex();
            if (index != -1) {
                String selected = inventoryListModel.get(index);
                deleteInventory(selected);
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um inventário!");
            }
        });
        btnPrintReport.addActionListener(e -> showInventoryReport());
        
        inventoryButtonPanel.add(btnCreateInventory);
        inventoryButtonPanel.add(btnDeleteInventory);
        inventoryButtonPanel.add(btnPrintReport);
        
        inventoryPanel.add(new JScrollPane(inventoryList), BorderLayout.CENTER);
        inventoryPanel.add(inventoryButtonPanel, BorderLayout.SOUTH);
        
        // Adicionar abas
        tabbedPane.addTab("Usuários", userPanel);
        tabbedPane.addTab("Setores", sectorPanel);
        tabbedPane.addTab("Itens", itemPanel);
        tabbedPane.addTab("Inventários", inventoryPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Botão de logout
        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> showLoginScreen());
        add(btnLogout, BorderLayout.SOUTH);
        
        // Carregar dados
        loadUsers();
        loadSectors();
        loadItems();
        loadInventories();
        
        revalidate();
        repaint();
    }
    
    private void showUserDashboard() {
        getContentPane().removeAll();
        setLayout(new BorderLayout());
        
        // Painel de itens
        JPanel itemPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddItem = new JButton("Adicionar Item");
        JButton btnEditItem = new JButton("Editar Item");
        JButton btnDeleteItem = new JButton("Excluir Item");
        JList<String> itemList = new JList<>(itemListModel);
        
        btnAddItem.addActionListener(e -> showAddItemDialog(currentUser.sector));
        btnEditItem.addActionListener(e -> {
            int index = itemList.getSelectedIndex();
            if (index != -1) {
                String selected = itemListModel.get(index);
                showEditItemDialog(selected);
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um item!");
            }
        });
        btnDeleteItem.addActionListener(e -> {
            int index = itemList.getSelectedIndex();
            if (index != -1) {
                String selected = itemListModel.get(index);
                deleteItem(selected);
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um item!");
            }
        });
        
        buttonPanel.add(btnAddItem);
        buttonPanel.add(btnEditItem);
        buttonPanel.add(btnDeleteItem);
        
        itemPanel.add(new JScrollPane(itemList), BorderLayout.CENTER);
        itemPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(itemPanel, BorderLayout.CENTER);
        
        // Botão de logout
        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> showLoginScreen());
        add(btnLogout, BorderLayout.SOUTH);
        
        // Carregar itens do setor do usuário
        loadItems(currentUser.sector);
        
        revalidate();
        repaint();
    }
    
    // ===================================================================
    // Métodos para Usuários
    // ===================================================================
    private void showAddUserDialog() {
        JDialog dialog = new JDialog(this, "Adicionar Usuário", true);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        dialog.setSize(400, 300);
        
        JTextField txtName = new JTextField();
        JTextField txtSector = new JTextField();
        JTextField txtEmail = new JTextField();
        JPasswordField txtPassword = new JPasswordField();
        JCheckBox chkAdmin = new JCheckBox("Administrador");
        JButton btnSave = new JButton("Salvar");
        
        dialog.add(new JLabel("Nome:"));
        dialog.add(txtName);
        dialog.add(new JLabel("Setor:"));
        dialog.add(txtSector);
        dialog.add(new JLabel("Email:"));
        dialog.add(txtEmail);
        dialog.add(new JLabel("Senha:"));
        dialog.add(txtPassword);
        dialog.add(new JLabel());
        dialog.add(chkAdmin);
        dialog.add(new JLabel());
        dialog.add(btnSave);
        
        btnSave.addActionListener(e -> {
            try {
                addUser(
                    txtName.getText(),
                    txtSector.getText(),
                    txtEmail.getText(),
                    new String(txtPassword.getPassword()),
                    chkAdmin.isSelected()
                );
                dialog.dispose();
                loadUsers();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Erro ao adicionar usuário: " + ex.getMessage());
            }
        });
        
        dialog.setVisible(true);
    }
    
    private void showEditUserDialog(String userInfo) {
        String email = userInfo.split("\\(")[1].split("\\)")[0];
        try {
            User user = getUserByEmail(email);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Usuário não encontrado!");
                return;
            }

            JDialog dialog = new JDialog(this, "Editar Usuário", true);
            dialog.setLayout(new GridLayout(6, 2, 10, 10));
            dialog.setSize(400, 300);

            JTextField txtName = new JTextField(user.name);
            JTextField txtSector = new JTextField(user.sector);
            JTextField txtEmail = new JTextField(user.email);
            JPasswordField txtPassword = new JPasswordField();
            JCheckBox chkAdmin = new JCheckBox("Administrador", user.isAdmin);
            JButton btnSave = new JButton("Salvar");

            dialog.add(new JLabel("Nome:"));
            dialog.add(txtName);
            dialog.add(new JLabel("Setor:"));
            dialog.add(txtSector);
            dialog.add(new JLabel("Email:"));
            dialog.add(txtEmail);
            dialog.add(new JLabel("Senha (nova):"));
            dialog.add(txtPassword);
            dialog.add(new JLabel());
            dialog.add(chkAdmin);
            dialog.add(new JLabel());
            dialog.add(btnSave);

            btnSave.addActionListener(e -> {
                try {
                    updateUser(
                        user.id,
                        txtName.getText(),
                        txtSector.getText(),
                        txtEmail.getText(),
                        new String(txtPassword.getPassword()),
                        chkAdmin.isSelected()
                    );
                    dialog.dispose();
                    loadUsers();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Erro ao atualizar usuário: " + ex.getMessage());
                }
            });

            dialog.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar usuário: " + ex.getMessage());
        }
    }
    
    private void deleteUser(String userInfo) {
        String email = userInfo.split("\\(")[1].split("\\)")[0];
        int confirm = JOptionPane.showConfirmDialog(
            this, 
            "Tem certeza que deseja excluir o usuário " + email + "?", 
            "Confirmação", 
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM users WHERE email = ?")) {
                
                stmt.setString(1, email);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    loadUsers();
                    JOptionPane.showMessageDialog(this, "Usuário excluído com sucesso!");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao excluir usuário: " + ex.getMessage());
            }
        }
    }
    
    private User getUserByEmail(String email) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, name, sector, is_admin FROM users WHERE email = ?")) {
            
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("sector"),
                    email,
                    rs.getBoolean("is_admin")
                );
            }
        }
        return null;
    }
    
    private void addUser(String name, String sector, String email, String password, boolean isAdmin) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO users (name, sector, email, password, is_admin) VALUES (?, ?, ?, ?, ?)")) {
            
            stmt.setString(1, name);
            stmt.setString(2, sector);
            stmt.setString(3, email);
            stmt.setString(4, password);
            stmt.setBoolean(5, isAdmin);
            stmt.executeUpdate();
        }
    }
    
    private void updateUser(int id, String name, String sector, String email, String password, boolean isAdmin) throws SQLException {
        String sql;
        if (password.isEmpty()) {
            sql = "UPDATE users SET name = ?, sector = ?, email = ?, is_admin = ? WHERE id = ?";
        } else {
            sql = "UPDATE users SET name = ?, sector = ?, email = ?, password = ?, is_admin = ? WHERE id = ?";
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, sector);
            stmt.setString(3, email);
            
            int paramIndex = 4;
            if (!password.isEmpty()) {
                stmt.setString(paramIndex++, password);
            }
            
            stmt.setBoolean(paramIndex++, isAdmin);
            stmt.setInt(paramIndex, id);
            stmt.executeUpdate();
        }
    }
    
    // ===================================================================
    // Métodos para Setores
    // ===================================================================
    private void showAddSectorDialog() {
        JDialog dialog = new JDialog(this, "Adicionar Setor", true);
        dialog.setLayout(new GridLayout(3, 2, 10, 10));
        dialog.setSize(300, 150);
        
        JTextField txtName = new JTextField();
        JButton btnSave = new JButton("Salvar");
        
        dialog.add(new JLabel("Nome do Setor:"));
        dialog.add(txtName);
        dialog.add(new JLabel());
        dialog.add(btnSave);
        
        btnSave.addActionListener(e -> {
            try {
                addSector(txtName.getText());
                dialog.dispose();
                loadSectors();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Erro ao adicionar setor: " + ex.getMessage());
            }
        });
        
        dialog.setVisible(true);
    }
    
    private void showEditSectorDialog(String sectorName) {
        JDialog dialog = new JDialog(this, "Editar Setor", true);
        dialog.setLayout(new GridLayout(3, 2, 10, 10));
        dialog.setSize(300, 150);
        
        JTextField txtName = new JTextField(sectorName);
        JButton btnSave = new JButton("Salvar");
        
        dialog.add(new JLabel("Nome do Setor:"));
        dialog.add(txtName);
        dialog.add(new JLabel());
        dialog.add(btnSave);
        
        btnSave.addActionListener(e -> {
            try {
                updateSector(sectorName, txtName.getText());
                dialog.dispose();
                loadSectors();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Erro ao atualizar setor: " + ex.getMessage());
            }
        });
        
        dialog.setVisible(true);
    }
    
    private void deleteSector(String sectorName) {
        int confirm = JOptionPane.showConfirmDialog(
            this, 
            "Tem certeza que deseja excluir o setor " + sectorName + "?", 
            "Confirmação", 
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM sectors WHERE name = ?")) {
                
                stmt.setString(1, sectorName);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    loadSectors();
                    JOptionPane.showMessageDialog(this, "Setor excluído com sucesso!");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao excluir setor: " + ex.getMessage());
            }
        }
    }
    
    private void addSector(String name) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO sectors (name) VALUES (?)")) {
            
            stmt.setString(1, name);
            stmt.executeUpdate();
        }
    }
    
    private void updateSector(String oldName, String newName) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE sectors SET name = ? WHERE name = ?")) {
            
            stmt.setString(1, newName);
            stmt.setString(2, oldName);
            stmt.executeUpdate();
        }
    }
    
    // ===================================================================
    // Métodos para Itens
    // ===================================================================
    private void showAddItemDialog(String defaultSector) {
        JDialog dialog = new JDialog(this, "Adicionar Item", true);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        dialog.setSize(400, 300);
        
        JTextField txtName = new JTextField();
        JTextField txtValue = new JTextField();
        JTextArea txtDescription = new JTextArea(3, 20);
        JScrollPane scrollPane = new JScrollPane(txtDescription);
        JComboBox<String> cmbSector = new JComboBox<>();
        JButton btnSave = new JButton("Salvar");
        
        // Carregar setores
        try {
            loadSectorsIntoCombo(cmbSector);
            if (defaultSector != null) {
                cmbSector.setSelectedItem(defaultSector);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(dialog, "Erro ao carregar setores: " + e.getMessage());
        }
        
        dialog.add(new JLabel("Nome:"));
        dialog.add(txtName);
        dialog.add(new JLabel("Valor:"));
        dialog.add(txtValue);
        dialog.add(new JLabel("Descrição:"));
        dialog.add(scrollPane);
        dialog.add(new JLabel("Setor:"));
        dialog.add(cmbSector);
        dialog.add(new JLabel());
        dialog.add(btnSave);
        
        btnSave.addActionListener(e -> {
            try {
                double value = Double.parseDouble(txtValue.getText());
                String sector = (String) cmbSector.getSelectedItem();
                addItem(
                    txtName.getText(),
                    value,
                    txtDescription.getText(),
                    sector
                );
                dialog.dispose();
                
                if (currentUser.isAdmin) {
                    loadItems();
                } else {
                    loadItems(currentUser.sector);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Valor inválido! Use números.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Erro ao adicionar item: " + ex.getMessage());
            }
        });
        
        dialog.setVisible(true);
    }
    
    private void showEditItemDialog(String itemInfo) {
        String itemName = itemInfo.split(" - ")[0];
        try {
            Item item = getItemByName(itemName);
            if (item == null) {
                JOptionPane.showMessageDialog(this, "Item não encontrado!");
                return;
            }

            JDialog dialog = new JDialog(this, "Editar Item", true);
            dialog.setLayout(new GridLayout(6, 2, 10, 10));
            dialog.setSize(400, 300);

            JTextField txtName = new JTextField(item.name);
            JTextField txtValue = new JTextField(String.valueOf(item.value));
            JTextArea txtDescription = new JTextArea(item.description, 3, 20);
            JScrollPane scrollPane = new JScrollPane(txtDescription);
            JComboBox<String> cmbSector = new JComboBox<>();
            JButton btnSave = new JButton("Salvar");

            // Carregar setores e selecionar o atual
            try {
                loadSectorsIntoCombo(cmbSector);
                cmbSector.setSelectedItem(getSectorNameById(item.sectorId));
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Erro ao carregar setores: " + e.getMessage());
            }

            dialog.add(new JLabel("Nome:"));
            dialog.add(txtName);
            dialog.add(new JLabel("Valor:"));
            dialog.add(txtValue);
            dialog.add(new JLabel("Descrição:"));
            dialog.add(scrollPane);
            dialog.add(new JLabel("Setor:"));
            dialog.add(cmbSector);
            dialog.add(new JLabel());
            dialog.add(btnSave);

            btnSave.addActionListener(e -> {
                try {
                    double value = Double.parseDouble(txtValue.getText());
                    String sector = (String) cmbSector.getSelectedItem();
                    updateItem(
                        item.id,
                        txtName.getText(),
                        value,
                        txtDescription.getText(),
                        sector
                    );
                    dialog.dispose();
                    
                    if (currentUser.isAdmin) {
                        loadItems();
                    } else {
                        loadItems(currentUser.sector);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Valor inválido! Use números.");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Erro ao atualizar item: " + ex.getMessage());
                }
            });

            dialog.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar item: " + ex.getMessage());
        }
    }
    
    private void deleteItem(String itemInfo) {
        String itemName = itemInfo.split(" - ")[0];
        int confirm = JOptionPane.showConfirmDialog(
            this, 
            "Tem certeza que deseja excluir o item " + itemName + "?", 
            "Confirmação", 
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM items WHERE name = ?")) {
                
                stmt.setString(1, itemName);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    if (currentUser.isAdmin) {
                        loadItems();
                    } else {
                        loadItems(currentUser.sector);
                    }
                    JOptionPane.showMessageDialog(this, "Item excluído com sucesso!");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao excluir item: " + ex.getMessage());
            }
        }
    }
    
    private Item getItemByName(String name) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, name, value, description, sector_id, user_id FROM items WHERE name = ?")) {
            
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new Item(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("value"),
                    rs.getString("description"),
                    rs.getInt("sector_id"),
                    rs.getInt("user_id")
                );
            }
        }
        return null;
    }
    
    private String getSectorNameById(int sectorId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM sectors WHERE id = ?")) {
            
            stmt.setInt(1, sectorId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("name");
            }
        }
        return null;
    }
    
    private void addItem(String name, double value, String description, String sector) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO items (name, value, description, sector_id, user_id) " +
                "VALUES (?, ?, ?, (SELECT id FROM sectors WHERE name = ?), ?)")) {
            
            stmt.setString(1, name);
            stmt.setDouble(2, value);
            stmt.setString(3, description);
            stmt.setString(4, sector);
            stmt.setInt(5, currentUser.id);
            stmt.executeUpdate();
        }
    }
    
    private void updateItem(int id, String name, double value, String description, String sector) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE items SET name = ?, value = ?, description = ?, " +
                "sector_id = (SELECT id FROM sectors WHERE name = ?) " +
                "WHERE id = ?")) {
            
            stmt.setString(1, name);
            stmt.setDouble(2, value);
            stmt.setString(3, description);
            stmt.setString(4, sector);
            stmt.setInt(5, id);
            stmt.executeUpdate();
        }
    }
    
    // ===================================================================
    // Métodos para Inventários
    // ===================================================================
    private void showCreateInventoryDialog() {
        JDialog dialog = new JDialog(this, "Criar Inventário", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 400);
        
        // Painel de seleção
        JPanel selectionPanel = new JPanel(new GridLayout(1, 2));
        
        // Lista de setores
        DefaultListModel<String> sectorModel = new DefaultListModel<>();
        JList<String> sectorList = new JList<>(sectorModel);
        
        // Lista de itens
        DefaultListModel<String> itemModel = new DefaultListModel<>();
        JList<String> itemList = new JList<>(itemModel);
        itemList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        selectionPanel.add(new JScrollPane(sectorList));
        selectionPanel.add(new JScrollPane(itemList));
        
        // Botões
        JPanel buttonPanel = new JPanel();
        JButton btnCreate = new JButton("Criar Inventário");
        buttonPanel.add(btnCreate);
        
        dialog.add(selectionPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Carregar dados
        loadSectors(sectorModel);
        sectorList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedSector = sectorList.getSelectedValue();
                if (selectedSector != null) {
                    loadItems(itemModel, selectedSector);
                }
            }
        });
        
        btnCreate.addActionListener(e -> {
            String selectedSector = sectorList.getSelectedValue();
            List<String> selectedItems = itemList.getSelectedValuesList();
            
            if (selectedSector == null || selectedItems.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Selecione um setor e pelo menos um item!");
                return;
            }
            
            try {
                createInventory(selectedSector, selectedItems);
                dialog.dispose();
                loadInventories();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Erro ao criar inventário: " + ex.getMessage());
            }
        });
        
        dialog.setVisible(true);
    }
    
    private void deleteInventory(String inventoryInfo) {
        int id = Integer.parseInt(inventoryInfo.split("#")[1].split(" - ")[0]);
        int confirm = JOptionPane.showConfirmDialog(
            this, 
            "Tem certeza que deseja excluir o inventário #" + id + "?", 
            "Confirmação", 
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Excluir itens associados primeiro
                try (PreparedStatement stmt = connection.prepareStatement(
                        "DELETE FROM inventory_items WHERE inventory_id = ?")) {
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                }
                
                // Excluir o inventário
                try (PreparedStatement stmt = connection.prepareStatement(
                        "DELETE FROM inventories WHERE id = ?")) {
                    stmt.setInt(1, id);
                    int rowsAffected = stmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        loadInventories();
                        JOptionPane.showMessageDialog(this, "Inventário excluído com sucesso!");
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao excluir inventário: " + ex.getMessage());
            }
        }
    }
    
    private void createInventory(String sector, List<String> items) throws SQLException {
        // Inserir inventário
        int inventoryId;
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO inventories (sector_id, user_id) " +
                "VALUES ((SELECT id FROM sectors WHERE name = ?), ?) " +
                "RETURNING id", Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, sector);
            stmt.setInt(2, currentUser.id);
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                inventoryId = rs.getInt(1);
            } else {
                throw new SQLException("Falha ao criar inventário");
            }
        }
        
        // Inserir itens do inventário
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO inventory_items (inventory_id, item_id) " +
                "VALUES (?, (SELECT id FROM items WHERE name = ?))")) {
            
            for (String item : items) {
                stmt.setInt(1, inventoryId);
                stmt.setString(2, item);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    
    private void showInventoryReport() {
        JDialog reportDialog = new JDialog(this, "Relatório de Inventário", true);
        reportDialog.setSize(800, 600);
        reportDialog.setLayout(new BorderLayout());

        // Modelo de tabela
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID");
        model.addColumn("Setor");
        model.addColumn("Responsável");
        model.addColumn("Data");
        model.addColumn("Itens");
        model.addColumn("Valor Total");

        // Preencher a tabela com dados do banco
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT i.id, s.name AS sector, u.name AS user_name, i.created_at, " +
                 "STRING_AGG(it.name, ', ') AS items, SUM(it.value) AS total_value " +
                 "FROM inventories i " +
                 "JOIN sectors s ON i.sector_id = s.id " +
                 "JOIN users u ON i.user_id = u.id " +
                 "JOIN inventory_items inv_it ON i.id = inv_it.inventory_id " +
                 "JOIN items it ON inv_it.item_id = it.id " +
                 "GROUP BY i.id, s.name, u.name, i.created_at")) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("sector"),
                    rs.getString("user_name"),
                    new SimpleDateFormat("dd/MM/yyyy HH:mm").format(rs.getTimestamp("created_at")),
                    rs.getString("items"),
                    String.format("R$ %.2f", rs.getDouble("total_value"))
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar relatório: " + e.getMessage());
        }

        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(300);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        
        JScrollPane scrollPane = new JScrollPane(table);
        reportDialog.add(scrollPane, BorderLayout.CENTER);
        
        // Botão de impressão
        JButton btnPrint = new JButton("Imprimir");
        btnPrint.addActionListener(e -> {
            try {
                table.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(reportDialog, "Erro ao imprimir: " + ex.getMessage());
            }
        });
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnPrint);
        reportDialog.add(buttonPanel, BorderLayout.SOUTH);

        reportDialog.setVisible(true);
    }
    
    // ===================================================================
    // Métodos auxiliares
    // ===================================================================
    private void loadSectorsIntoCombo(JComboBox<String> combo) throws SQLException {
        combo.removeAllItems();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sectors")) {
            
            while (rs.next()) {
                combo.addItem(rs.getString("name"));
            }
        }
    }
    
    private void loadUsers() {
        userListModel.clear();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, email, sector FROM users")) {
            
            while (rs.next()) {
                userListModel.addElement(String.format("%s (%s) - %s", 
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("sector")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar usuários: " + e.getMessage());
        }
    }
    
    private void loadSectors() {
        sectorListModel.clear();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sectors")) {
            
            while (rs.next()) {
                sectorListModel.addElement(rs.getString("name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar setores: " + e.getMessage());
        }
    }
    
    private void loadSectors(DefaultListModel<String> model) {
        model.clear();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sectors")) {
            
            while (rs.next()) {
                model.addElement(rs.getString("name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar setores: " + e.getMessage());
        }
    }
    
    private void loadItems() {
        itemListModel.clear();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT i.name, i.value, s.name AS sector_name " +
                 "FROM items i JOIN sectors s ON i.sector_id = s.id")) {
            
            while (rs.next()) {
                itemListModel.addElement(String.format("%s - R$%.2f [%s]", 
                    rs.getString("name"),
                    rs.getDouble("value"),
                    rs.getString("sector_name")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar itens: " + e.getMessage());
        }
    }
    
    private void loadItems(String sector) {
        itemListModel.clear();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT i.name, i.value, i.description " +
                "FROM items i " +
                "JOIN sectors s ON i.sector_id = s.id " +
                "WHERE s.name = ?")) {
            
            stmt.setString(1, sector);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                itemListModel.addElement(String.format("%s - R$%.2f", 
                    rs.getString("name"),
                    rs.getDouble("value")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar itens: " + e.getMessage());
        }
    }
    
    private void loadItems(DefaultListModel<String> model, String sector) {
        model.clear();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM items " +
                "WHERE sector_id = (SELECT id FROM sectors WHERE name = ?)")) {
            
            stmt.setString(1, sector);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                model.addElement(rs.getString("name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar itens: " + e.getMessage());
        }
    }
    
    private void loadInventories() {
        inventoryListModel.clear();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT i.id, s.name AS sector, u.name AS user, i.created_at " +
                 "FROM inventories i " +
                 "JOIN sectors s ON i.sector_id = s.id " +
                 "JOIN users u ON i.user_id = u.id")) {
            
            while (rs.next()) {
                inventoryListModel.addElement(String.format("Inventário #%d - %s (%s) - %s", 
                    rs.getInt("id"),
                    rs.getString("sector"),
                    rs.getString("user"),
                    new SimpleDateFormat("dd/MM/yyyy HH:mm").format(rs.getTimestamp("created_at"))));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar inventários: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new InventorySystem().setVisible(true);
        });
    }
    
    // ===================================================================
    // Classes de modelo
    // ===================================================================
    static class User {
        int id;
        String name;
        String sector;
        String email;
        boolean isAdmin;
        
        public User(int id, String name, String sector, String email, boolean isAdmin) {
            this.id = id;
            this.name = name;
            this.sector = sector;
            this.email = email;
            this.isAdmin = isAdmin;
        }
    }
    
    static class Item {
        int id;
        String name;
        double value;
        String description;
        int sectorId;
        int userId;
        
        public Item(int id, String name, double value, String description, int sectorId, int userId) {
            this.id = id;
            this.name = name;
            this.value = value;
            this.description = description;
            this.sectorId = sectorId;
            this.userId = userId;
        }
    }
}