package br.com.pedrodouglas.gerarexcel.service;

import br.com.pedrodouglas.gerarexcel.model.Nfse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class NfseService {
    public List<Nfse> parseNfseData(String xml) {
        try {
            // Convertendo a String XML para um InputStream usando ByteArrayInputStream
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes("UTF-8"));

            // Criando o Document a partir do InputStream
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(inputStream));
            NodeList nfseNodes = document.getElementsByTagName("GerarNfseResposta");

            List<Nfse> nfseList = new ArrayList<>();
            for (int i = 0; i < nfseNodes.getLength(); i++) {
                Node node = nfseNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element nfseElement = (Element) node;
                    Nfse nfse = new Nfse();
                    nfse.setNumero(nfseElement.getElementsByTagName("Numero").item(0).getTextContent());
                    nfse.setValor(Double.parseDouble(nfseElement.getElementsByTagName("ValorServicos").item(0).getTextContent()));
                    nfse.setPis(Double.parseDouble(nfseElement.getElementsByTagName("ValorPis").item(0).getTextContent()));
                    nfse.setCofins(Double.parseDouble(nfseElement.getElementsByTagName("ValorCofins").item(0).getTextContent()));
                    nfse.setIrpj(Double.parseDouble(nfseElement.getElementsByTagName("ValorIr").item(0).getTextContent()));
                    nfse.setCsll(Double.parseDouble(nfseElement.getElementsByTagName("ValorCsll").item(0).getTextContent()));
                    nfse.setIss(Double.parseDouble(nfseElement.getElementsByTagName("ValorIss").item(0).getTextContent()));
                    nfse.setInss(Double.parseDouble(nfseElement.getElementsByTagName("ValorInss").item(0).getTextContent()));
                    nfse.setSituacao(nfseElement.getElementsByTagName("Codigo").item(0).getTextContent());
                    nfseList.add(nfse);
                }
            }
            return nfseList;

        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] gerarExcel(List<Nfse> nfseList) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Notas Fiscais");

            // Adicionando títulos às colunas
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Nota Fiscal");
            headerRow.createCell(1).setCellValue("Valor");
            headerRow.createCell(2).setCellValue("PIS");
            headerRow.createCell(3).setCellValue("COFINS");
            headerRow.createCell(4).setCellValue("IRPJ");
            headerRow.createCell(5).setCellValue("CSLL");
            headerRow.createCell(6).setCellValue("ISS");
            headerRow.createCell(7).setCellValue("INSS");

            int rowNum = 1;
            double valorTotal = 0;
            double pisNotaFiscal = 0;
            double totalCofinsNota = 0;

            for (Nfse nfse : nfseList) {
                Row row = sheet.createRow(rowNum++);

                if(nfse.getSituacao().equalsIgnoreCase("L040") ||  nfse.getSituacao().equalsIgnoreCase("L050")){
                    nfse.setValor(0);
                    nfse.setPis(0);
                    nfse.setCofins(0);
                    nfse.setIrpj(0);
                    nfse.setCsll(0);
                    nfse.setInss(0);
                    nfse.setIss(0);
                }


                row.createCell(0).setCellValue(nfse.getNumero());
                row.createCell(1).setCellValue(nfse.getValor());
                row.createCell(2).setCellValue(nfse.getPis());
                row.createCell(3).setCellValue(nfse.getCofins());
                row.createCell(4).setCellValue(nfse.getIrpj());
                row.createCell(5).setCellValue(nfse.getCsll());
                row.createCell(6).setCellValue(Objects.isNull(nfse.getIss()) ? 0 : nfse.getIss());
                row.createCell(7).setCellValue(Objects.isNull(nfse.getInss()) ? 0 : nfse.getInss());

                valorTotal += nfse.getValor();
                pisNotaFiscal += nfse.getPis(); // Adiciona o valor de PIS para o total
                totalCofinsNota += nfse.getCofins(); // Adiciona o valor de COFINS para o total
            }

            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(0).setCellValue("Totais");
            for (int i = 1; i <= 7; i++) {
                // Calcular e adicionar os totais das colunas 1 a 7
                CellReference startCellRef = new CellReference(1, i, false, false);
                CellReference endCellRef = new CellReference(rowNum - 1, i, false, false);

                String formula = "SUM(" + startCellRef.formatAsString() + ":" + endCellRef.formatAsString() + ")";
                totalRow.createCell(i).setCellFormula(formula);
            }

            int rowNumFinal = rowNum + 2;
            Row pis = sheet.createRow(rowNumFinal);

            double pisTotal = valorTotal * 0.0065;
            pisTotal = Math.round(pisTotal * 100.0) / 100.0;

            pis.createCell(0).setCellValue("pis");
            pis.createCell(1).setCellValue(pisTotal);

            Row pisRetido = sheet.createRow(rowNumFinal + 1);
            pisRetido.createCell(0).setCellValue("pis retido");
            pisRetido.createCell(1).setCellValue(pisNotaFiscal);

            Row pisARecolher = sheet.createRow(rowNumFinal + 2);
            pisARecolher.createCell(0).setCellValue("pis a recolher");
            pisARecolher.createCell(1).setCellValue(pisTotal - pisNotaFiscal);

            Row cofins = sheet.createRow(rowNumFinal + 4);
            double totalConfis = valorTotal * 0.03;
            totalConfis = Math.round(totalConfis * 100.0) / 100.0;
            cofins.createCell(0).setCellValue("cofins");
            cofins.createCell(1).setCellValue(totalConfis);

            Row confisRetido = sheet.createRow(rowNumFinal + 5);
            confisRetido.createCell(0).setCellValue("cofins retido");
            confisRetido.createCell(1).setCellValue(totalCofinsNota);

            Row cofinsARecolher = sheet.createRow(rowNumFinal + 6);
            cofinsARecolher.createCell(0).setCellValue("cofins a recolher");
            cofinsARecolher.createCell(1).setCellValue(totalConfis - totalCofinsNota);



            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    public byte[] gerarCSV(List<Nfse> nfseList) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(outputStream), CSVFormat.DEFAULT)) {

            // Adicione cabeçalhos ao CSV
            csvPrinter.printRecord("Nota Fiscal", "Valor", "PIS", "COFINS", "IRPJ", "CSLL","ISS", "INSS");

            // Adicione dados ao CSV
            for (Nfse nfse : nfseList) {
                csvPrinter.printRecord(nfse.getNumero(), nfse.getValor(), nfse.getPis(), nfse.getCofins(), nfse.getIrpj(), nfse.getCsll(), nfse.getInss());
            }

            csvPrinter.flush();
            return outputStream.toByteArray();
        }
    }
}