package com.sugyeong.churavel.photo;

import com.sugyeong.churavel.common.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class LocalPhotoStorage implements PhotoStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalPhotoStorage.class);
    private static final Pattern SAFE_NAME = Pattern.compile("[0-9a-f\\-]{36}\\.[a-z0-9]{1,5}");

    private final Path root;

    public LocalPhotoStorage(StorageProperties props) {
        this.root = props.photoDir().toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("사진 폴더를 만들지 못했습니다: " + root, e);
        }
        log.info("사진 저장 위치: {}", root);
    }

    @Override
    public String save(InputStream in, String extension) {
        String name = UUID.randomUUID() + "." + extension;
        try {
            Files.copy(in, root.resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException("사진을 저장하지 못했습니다", e);
        }
        return name;
    }

    @Override
    public Resource load(String storedName) {
        Path file = resolve(storedName);
        if (!Files.isRegularFile(file)) {
            throw new NotFoundException("사진 파일이 없습니다");
        }
        return new FileSystemResource(file);
    }

    @Override
    public void delete(String storedName) {
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException e) {
            // 파일 정리는 실패해도 요청은 성공으로 둔다
            log.warn("사진 파일 삭제 실패: {}", storedName, e);
        }
    }

    private Path resolve(String storedName) {
        if (!SAFE_NAME.matcher(storedName).matches()) {
            throw new IllegalArgumentException("잘못된 파일 이름입니다");
        }
        return root.resolve(storedName);
    }
}
