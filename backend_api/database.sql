CREATE TABLE `admin` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
);

INSERT INTO `admin` (`username`, `password_hash`, `role`) VALUES
('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'ADMIN'), -- est2024
('bendahara', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'BENDAHARA'),
('developer', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'DEVELOPER');

CREATE TABLE `anggota` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nra` varchar(50) NOT NULL,
  `nama` varchar(100) NOT NULL,
  `alias` varchar(100) DEFAULT NULL,
  `telepon` varchar(20) DEFAULT NULL,
  `statusAktif` tinyint(1) NOT NULL DEFAULT 1,
  `tanggalLahir` varchar(20) DEFAULT NULL,
  `uangKas` double NOT NULL DEFAULT 0,
  `iuranAniv` double NOT NULL DEFAULT 0,
  `sisaCicilan` double NOT NULL DEFAULT 0,
  `hargaBarang` double NOT NULL DEFAULT 0,
  `namaBarang` varchar(100) DEFAULT NULL,
  `jabatan` varchar(50) DEFAULT NULL,
  `password` varchar(255) NOT NULL DEFAULT '12345',
  PRIMARY KEY (`id`)
);

CREATE TABLE `pembayaran` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `firestoreId` varchar(100) DEFAULT NULL,
  `anggotaId` int(11) NOT NULL,
  `anggotaNama` varchar(100) NOT NULL,
  `jenisPembayaran` varchar(50) NOT NULL,
  `nominal` double NOT NULL,
  `tanggal` bigint(20) NOT NULL,
  `keterangan` text DEFAULT NULL,
  `buktiPembayaran` text DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `pengeluaran` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `keterangan` text NOT NULL,
  `nominal` double NOT NULL,
  `tanggal` bigint(20) NOT NULL,
  `jenisKas` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `kas_keliling` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nominal` double NOT NULL,
  `tanggal` bigint(20) NOT NULL,
  `keterangan` text DEFAULT NULL,
  `jenisTransaksi` varchar(50) NOT NULL,
  `bulan` varchar(50) DEFAULT NULL,
  `tahun` varchar(20) DEFAULT NULL,
  `totalPemasukan` double NOT NULL DEFAULT 0,
  `totalPengeluaran` double NOT NULL DEFAULT 0,
  `saldoBulan` double NOT NULL DEFAULT 0,
  `catatan` text DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `catatan` (
  `id` bigint(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,
  `timestamp` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `daftar_hadir` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `judul` varchar(255) NOT NULL,
  `tanggal` bigint(20) NOT NULL,
  `tipe` varchar(50) NOT NULL,
  `lokasi` varchar(255) DEFAULT NULL,
  `catatan` text DEFAULT NULL,
  `pesertaHadir` text, -- JSON array of Anggota IDs
  PRIMARY KEY (`id`)
);

CREATE TABLE `community_settings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `community_name` varchar(255) DEFAULT 'NEBO SUKABUMI',
  `community_slogan` varchar(255) DEFAULT 'Solidaritas Tanpa Batas',
  `community_motto` varchar(255) DEFAULT 'Satu Aspal Satu Tujuan',
  `community_logo` text DEFAULT NULL,
  `community_banner` text DEFAULT NULL,
  `community_splash` text DEFAULT NULL,
  `community_address` text DEFAULT NULL,
  `community_phone` varchar(50) DEFAULT NULL,
  `community_email` varchar(100) DEFAULT NULL,
  `community_website` varchar(100) DEFAULT NULL,
  `community_facebook` varchar(100) DEFAULT NULL,
  `community_instagram` varchar(100) DEFAULT NULL,
  `community_youtube` varchar(100) DEFAULT NULL,
  `updated_at` bigint(20) DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

INSERT INTO `community_settings` (`id`) VALUES (1);

CREATE TABLE `activity_logs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `timestamp` bigint(20) NOT NULL,
  `user_role` varchar(50) NOT NULL,
  `action` text NOT NULL,
  PRIMARY KEY (`id`)
);

