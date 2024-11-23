package pe.edu.upeu.pppmanager.service;

import java.io.IOException;
import java.sql.SQLException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
@Service
public interface ExcelService {
    void importData(MultipartFile file) throws IOException, SQLException;
}
