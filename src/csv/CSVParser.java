package csv;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.ArrayList;

public class CSVParser {

    enum ParseState {
        Start,
        ParsingUnquotedCell,
        ParsingQuotedCell,
        ParsingQuotedCell_AfterQuote,
        AfterCarriageReturn,
        EndOfCell,
        EndOfRecord,
        EndOfFile,
    }

    public static class CSVException extends Exception {
        public CSVException() {}

        public CSVException(String message) {
            super(message);
        }

        public CSVException(Throwable cause) {
            super(cause);
        }

        public CSVException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public CSVParser() {
    }

    public List<List<String>> ParseFile(File file) throws CSVException, IOException {
        Reader input_stream = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        return ParseStream(input_stream);
    }

    public List<List<String>> ParseFile(File file, Charset charset) throws CSVException, IOException {
        Reader input_stream = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
        return ParseStream(input_stream);
    }

    public List<List<String>> ParseString(String string) throws CSVException, IOException {
        StringReader input_stream = new StringReader(string);
        return ParseStream(input_stream);
    }

    public List<List<String>> ParseStream(Reader stream) throws CSVException, IOException {
        if (!stream.markSupported()) {
            throw new CSVException("Stream must support mark!");
        }

        List<List<String>> records = new ArrayList<>();

        List<String> record = new ArrayList<>();
        ParseState final_state = ParseNextRecord(stream, record);
        records.add(record);

        while (final_state != ParseState.EndOfFile) {
            record = new ArrayList<>();
            final_state = ParseNextRecord(stream, record);
            records.add(record);
        }

        return records;
    }

    ParseState ParseNextRecord(Reader in, List<String> record) throws CSVException, IOException {
        StringBuilder cell = new StringBuilder();

        in.mark(1);
        int current = in.read();

        ParseState state = ParseState.Start;
        while (state != ParseState.EndOfRecord && state != ParseState.EndOfFile) {
            switch (state) {
                case Start -> state = State_Start(in, current, record, cell);
                case ParsingUnquotedCell -> state = State_ParsingUnquotedCell(in, current, record, cell);
                case ParsingQuotedCell -> state = State_ParsingQuotedCell(in, current, record, cell);
                case ParsingQuotedCell_AfterQuote -> state = State_ParsingQuotedCell_AfterQuote(in, current, record, cell);
                case AfterCarriageReturn -> state = State_AfterCarriageReturn(in, current, record, cell);
                case EndOfCell -> state = State_EndOfCell(in, current, record, cell);
            }

            in.mark(1);
            current = in.read();
        }

        State_EndOfCell(in, current, record, cell);

        return state;
    }

    ParseState State_Start(Reader in, int current, List<String> record, StringBuilder cell) {
        switch (current) {
            case '"':
                return ParseState.ParsingQuotedCell;
            case ',':
                return ParseState.EndOfCell;
            case -1:
                return ParseState.EndOfFile;
            case '\n':
                return ParseState.EndOfRecord;
            case '\r':
                return ParseState.AfterCarriageReturn;
            default:
                cell.append((char) current);
                return ParseState.ParsingUnquotedCell;
        }
    }

    ParseState State_ParsingUnquotedCell(Reader in, int current, List<String> record, StringBuilder cell) {
        switch (current) {
            case ',':
                return ParseState.EndOfCell;
            case -1:
                return ParseState.EndOfFile;
            case '\n':
                return ParseState.EndOfRecord;
            case '\r':
                return ParseState.AfterCarriageReturn;
            default:
                cell.append((char) current);
                return ParseState.ParsingUnquotedCell;
        }
    }

    ParseState State_ParsingQuotedCell(Reader in, int current, List<String> record, StringBuilder cell) throws CSVException {
        switch (current) {
            case '"':
                return ParseState.ParsingQuotedCell_AfterQuote;
            case -1:
                throw new CSVException("Invalid CSV format: data ended before matching quote");
            default:
                cell.append((char) current);
                return ParseState.ParsingQuotedCell;
        }
    }

    ParseState State_ParsingQuotedCell_AfterQuote(Reader in, int current, List<String> record, StringBuilder cell) throws CSVException {
        switch (current) {
            case '"':
                cell.append('"');
                return ParseState.ParsingQuotedCell;
            case ',':
                return ParseState.EndOfCell;
            case -1:
                return ParseState.EndOfFile;
            case '\n':
                return ParseState.EndOfRecord;
            case '\r':
                return ParseState.AfterCarriageReturn;
            default:
                throw new CSVException("Invalid CSV format: quote must be followed by quote, cell delimiter, or record delimiter (was `" + (char)current + "`)");
        }
    }

    ParseState State_AfterCarriageReturn(Reader in, int current, List<String> record, StringBuilder cell) throws java.io.IOException {
        switch (current) {
            case -1:
                return ParseState.EndOfFile;
            case '\n':
                return ParseState.EndOfRecord;
            default:
                in.reset();
                return ParseState.EndOfRecord;
        }
    }

    ParseState State_EndOfCell(Reader in, int current, List<String> record, StringBuilder cell) throws java.io.IOException {
        record.add(cell.toString());
        cell.setLength(0);
        in.reset();

        return ParseState.Start;
    }

}
