package pe.edu.upeu.pppmanager.ServiceImpl;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import pe.edu.upeu.pppmanager.service.ExcelService;
@Service
public class ExcelServiceImpl implements ExcelService {


    private final DataSource dataSource;

    public ExcelServiceImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void importData(MultipartFile file) throws IOException, SQLException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream());
             Connection conn = dataSource.getConnection()) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            Map<String, Integer> columnIndices = getColumnIndices(headerRow);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (isRowEmpty(row)) {
                    continue;
                }

                String apellido = getCellValueAsString(row.getCell(columnIndices.get("Apellido")));
                String nombre = getCellValueAsString(row.getCell(columnIndices.get("Nombre")));
                String dni = getCellValueAsString(row.getCell(columnIndices.get("DNI")));

                if (dni == null || dni.trim().isEmpty() || existeDni(conn, dni)) {
                    continue;
                }

                String correo = getCellValueAsString(row.getCell(columnIndices.get("Correo")));
                if (correo != null && existeCorreo(conn, correo)) {
                    continue;
                }

                String telefono = getCellValueAsString(row.getCell(columnIndices.get("Telefono")));
                if (telefono == null || telefono.trim().isEmpty()) {
                    telefono = "Sin telefono";
                }

                String codigo = getCellValueAsString(row.getCell(columnIndices.get("Codigo")));
                if (codigo != null && existeCodigo(conn, codigo)) {
                    continue;
                }

                int idPersona = insertarPersona(conn, apellido, nombre, dni, correo, telefono, "A");
                insertarEstudiante(conn, idPersona, codigo, "A");
            }
        }
    }


    private Map<String, Integer> getColumnIndices(Row headerRow) {
        Map<String, Integer> columnIndices = new HashMap<>();
        for (Cell cell : headerRow) {
            columnIndices.put(cell.getStringCellValue(), cell.getColumnIndex());
        }
        return columnIndices;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int cellNum = 0; cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValueAsString(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean existeDni(Connection conn, String dni) throws SQLException {
        String consulta = "SELECT 1 FROM PERSONA WHERE DNI = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(consulta)) {
            pstmt.setString(1, dni);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    private boolean existeCorreo(Connection conn, String correo) throws SQLException {
        String consulta = "SELECT 1 FROM PERSONA WHERE CORREO = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(consulta)) {
            pstmt.setString(1, correo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean existeCodigo(Connection conn, String codigo) throws SQLException {
        String consulta = "SELECT 1 FROM ESTUDIANTE WHERE CODIGO = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(consulta)) {
            pstmt.setString(1, codigo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    private int getCellValueAsInt(Cell cell) {
        if (cell == null) {
            return 0;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Integer.parseInt(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private int insertarPersona(Connection conn, String apellido, String nombre, String dni, String correo, String telefono, String estado) throws SQLException {
        String insertPersonaSQL = "INSERT INTO PERSONA (APELLIDO, NOMBRE, DNI, CORREO, TELEFONO, ESTADO) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertPersonaSQL, new String[]{"ID_PERSONA"})) {
            pstmt.setString(1, apellido);
            pstmt.setString(2, nombre);
            pstmt.setString(3, dni);
            pstmt.setString(4, correo);
            pstmt.setString(5, telefono);
            pstmt.setString(6, estado);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Error al insertar en PERSONA.");
    }

    private void insertarEstudiante(Connection conn, int idPersona,String codigo,String estado) throws SQLException {
        String insertEstudianteSQL = "INSERT INTO ESTUDIANTE (ID_PERSONA, CODIGO, ESTADO) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertEstudianteSQL)) {
            pstmt.setInt(1, idPersona);
            pstmt.setString(2, codigo);
            pstmt.setString(3, estado);
            pstmt.executeUpdate();
        }
    }
}
