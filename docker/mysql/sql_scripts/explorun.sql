-- phpMyAdmin SQL Dump
-- version 5.0.2
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1:3306
-- Generation Time: Aug 27, 2022 at 08:02 AM
-- Server version: 5.7.31
-- PHP Version: 7.3.21

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `explorun`
--

-- --------------------------------------------------------

--
-- Table structure for table `badge_info`
--

DROP TABLE IF EXISTS `badge_info`;
CREATE TABLE IF NOT EXISTS `badge_info` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL,
  `description` varchar(2000) NOT NULL,
  `minimum_val` float NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=9 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `badge_info`
--

INSERT INTO `badge_info` (`id`, `name`, `description`, `minimum_val`) VALUES
(1, 'NATURE', 'Total distance(meters) covered in nature in a week', 10000),
(2, 'FORREST GUMP', 'Time spent(minutes) running in a week', 600),
(3, 'HIGH INTENSITY', 'High average pace(km/h) reached in a week', 16.1),
(4, 'MOUNTAINEER', 'Elevation(meters) covered in a week', 1000),
(5, 'LAZY', 'Very small distance covered in a week(meters), minimum_val means max here', 7000),
(6, 'SURRENDER', 'Run not finished', -1),
(7, 'CRAWLER', 'Never touched the average running speed(km/h) in a week', 12),
(8, 'CHALLENGE COMPLETED', 'Challenge of the week done', -1);

-- --------------------------------------------------------

--
-- Table structure for table `fixed_preferences`
--

DROP TABLE IF EXISTS `fixed_preferences`;
CREATE TABLE IF NOT EXISTS `fixed_preferences` (
  `user_id` int(10) NOT NULL AUTO_INCREMENT,
  `length` float NOT NULL,
  `elevation` float NOT NULL,
  `ped_friend` float NOT NULL,
  `uniqueness` float NOT NULL,
  `nature` float NOT NULL,
  `avg_num` int(10) NOT NULL DEFAULT '1',
  PRIMARY KEY (`user_id`)
) ENGINE=MyISAM AUTO_INCREMENT=4001 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `fixed_preferences`
--

INSERT INTO `fixed_preferences` (`user_id`, `length`, `elevation`, `ped_friend`, `uniqueness`, `nature`, `avg_num`) VALUES
(1, 0.111111, 0.0990991, 0.76, 0.64, 0.88, 1),
(2, 0.111111, 0.0990991, 0.76, 0.64, 0.88, 1),
(7, 0.238889, 0.379379, 1, 0.8, 0, 1),
(5, 0.522222, 0.319319, 0, 0.8, 0.8, 1),
(3, 0.311111, 0.60961, 1, 0.6, 0.5, 1),
(4, 0.111111, 0.149149, 0.61, 0.64, 0.88, 2),
(6, 0.622222, 0.279279, 0.6, 0.2, 0.3, 1),
(8, 0.0166667, 0.159159, 0.5, 0.3, 0.3, 1),
(9, 0.335242, 0.460932, 0.687379, 0.671359, 0.64879, 2),
(10, 0.0944444, 0.309309, 0.4, 0.5, 0.5, 1),
(11, 0.117939, 0.118466, 0.385185, 0.57037, 0.414774, 2),
(12, 0.35, 0.54955, 0.2, 0.3, 1, 1),
(13, 0.111111, 0.149149, 0.2, 0.3, 1, 1),
(14, 0.127778, 0.329329, 0.1, 0.1, 1, 1),
(15, 0, 0.139139, 0.2, 0.3, 1, 1),
(16, 0.183333, 0.309309, 0.2, 0.3, 0.5, 1),
(18, 0.105556, 0.319319, 0.5, 0.2, 0.6, 1),
(19, 0.111111, 0.0990991, 0.76, 0.64, 0.88, 1),
(20, 0.111111, 0.0990991, 0.76, 0.64, 0.88, 1);

-- --------------------------------------------------------

--
-- Table structure for table `loginfo`
--

DROP TABLE IF EXISTS `loginfo`;
CREATE TABLE IF NOT EXISTS `loginfo` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `username` varchar(2000) NOT NULL,
  `password` varchar(2000) NOT NULL,
  `firstlogin` int(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=21 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `loginfo`
--

INSERT INTO `loginfo` (`id`, `username`, `password`, `firstlogin`) VALUES
(1, 'vivek', 'dsbhfdkdhfjs', 1),
(2, 'laser', 'password', 1),
(3, 'user0', '1234', 1),
(4, 'sampleuser', '1234', 1),
(5, 'user4', '1234', 1),
(6, 'user5', '1234', 1),
(7, 'user7', '1234', 1),
(8, 'user10', '1234', 1),
(9, 'a', '1', 1),
(10, 'b', '1', 1),
(11, 'c', '1', 1),
(12, 'e', '1', 1),
(13, 'd', '1', 1),
(14, 'f', '1', 1),
(15, 'g', '1', 1),
(16, 'h', '1', 1),
(18, 'p', '1', 1),
(19, 'k', '1', 1),
(20, 'm', '1', 1);

-- --------------------------------------------------------

--
-- Table structure for table `run_info`
--

DROP TABLE IF EXISTS `run_info`;
CREATE TABLE IF NOT EXISTS `run_info` (
  `user_id` int(10) NOT NULL,
  `distance` int(10) DEFAULT NULL,
  `nature_D` int(10) DEFAULT NULL,
  `urb_D` int(10) DEFAULT NULL,
  `kcal` float NOT NULL DEFAULT '0',
  `xp` float NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Dumping data for table `run_info`
--

INSERT INTO `run_info` (`user_id`, `distance`, `nature_D`, `urb_D`, `kcal`, `xp`) VALUES
(19, 9000, 30000, 6000, 1368, 10140),
(20, 9000, 3000, 6000, 1368, 10140);

-- --------------------------------------------------------

--
-- Table structure for table `user_info`
--

DROP TABLE IF EXISTS `user_info`;
CREATE TABLE IF NOT EXISTS `user_info` (
  `user_id` int(10) NOT NULL,
  `sex` tinyint(1) NOT NULL DEFAULT '0',
  `age` int(3) NOT NULL DEFAULT '25',
  `height` float NOT NULL DEFAULT '160',
  `weight` float NOT NULL DEFAULT '70',
  PRIMARY KEY (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Dumping data for table `user_info`
--

INSERT INTO `user_info` (`user_id`, `sex`, `age`, `height`, `weight`) VALUES
(19, 0, 27, 170, 65),
(20, 0, 27, 170, 65);

-- --------------------------------------------------------

--
-- Table structure for table `user_records`
--

DROP TABLE IF EXISTS `user_records`;
CREATE TABLE IF NOT EXISTS `user_records` (
  `user_id` int(10) NOT NULL,
  `longest_distance` float NOT NULL,
  `best_pace` float NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Dumping data for table `user_records`
--

INSERT INTO `user_records` (`user_id`, `longest_distance`, `best_pace`) VALUES
(20, 3000, 13.2);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
