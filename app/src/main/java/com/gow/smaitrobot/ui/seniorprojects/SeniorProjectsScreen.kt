package com.gow.smaitrobot.ui.seniorprojects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gow.smaitrobot.ui.common.WieBackground

/**
 * Senior Projects screen — vertical scroll, one supervisor block at a time
 * matching the Mechanical Engineering Senior Projects brochure layout.
 *
 *  Header
 *  ──────
 *  Supervisor: Dr. Agarwal   ←  big SJSU navy
 *  ◯ Project Title         ←  gold heavy
 *    Team members          ←  muted dark
 *  ◯ Supervisor: Dr. Armani
 *  ...
 */
@Composable
fun SeniorProjectsScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    WieBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.DarkGray,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Mechanical Engineering",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.DarkGray,
                        lineHeight = 48.sp
                    )
                    Text(
                        text = "Senior Projects",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray,
                        lineHeight = 36.sp
                    )
                }
            }

            // Gold rule under header
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 16.dp)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(scheme.secondary)
            )

            // Single scrollable column — one supervisor block after another
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(28.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SeniorProjectsData.allGroups) { group ->
                    SupervisorSection(group)
                }
            }
        }
    }
}

@Composable
private fun SupervisorSection(group: SupervisorGroup) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        // "Supervisor:" small label + big name (brochure pattern)
        Text(
            text = "Supervisor:",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray
        )
        Text(
            text = group.supervisor,
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.DarkGray,
            lineHeight = 60.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        group.projects.forEach { project ->
            ProjectRow(project)
        }
    }
}

@Composable
private fun ProjectRow(project: SeniorProject) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Brochure-style empty navy ring marker
        Box(
            modifier = Modifier
                .padding(top = 6.dp, end = 16.dp)
                .size(28.dp)
                .clip(CircleShape)
                .border(width = 3.dp, color = Color.DarkGray, shape = CircleShape)
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = project.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                lineHeight = 30.sp
            )
            Text(
                text = project.team,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Color.DarkGray.copy(alpha = 0.82f),
                lineHeight = 24.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

data class SeniorProject(val title: String, val team: String)
data class SupervisorGroup(val supervisor: String, val projects: List<SeniorProject>)

object SeniorProjectsData {

    private val agarwal = SupervisorGroup(
        "Dr. Agarwal",
        listOf(
            SeniorProject(
                "Net Deployment with Synchronous Drones",
                "Ethan Muzzio, Austin Leporini, Joshua Ng, and Caleb James"
            ),
            SeniorProject(
                "Redundant Electronic Throttle & Brake Assist for Paraplegic Driver",
                "Muhammed Shah, Raymond Alexander, Yuhao Chen, Darion de la Cruz, John King, and Davis Michael Salmon"
            ),
            SeniorProject(
                "Powered Zipline Carrier",
                "Eduardo Garcia Escovedo, Nicholas Joseph Borg, Raiden De Luna, Dylan Maloney, Ahan Patel, and Milan Schreurs"
            ),
            SeniorProject(
                "Trash Catching Vacuum Robot",
                "Patrick Hau Wong, Gustavo Ornelas, Alejandro Cedeno, Kristopher Anh Kiet Do, and Branden Tan Tran"
            ),
            SeniorProject(
                "Electric Therapeutic Wheelchair",
                "Josmar Vega Hernandez, Victor Manuel Jimenez, Benjamin Xu Nguyen, RJ Villegas, Ely L. Lopez, and Arjay Savellano"
            )
        )
    )

    private val armani = SupervisorGroup(
        "Dr. Armani",
        listOf(
            SeniorProject(
                "PEEK Interface Screw for ACL Reconstruction",
                "Ethan Belleh, Lucas Peregrino, Josue Antonio Luis, and Cristina Pineda Carranza"
            ),
            SeniorProject(
                "Affordable Smart Wheelchair With Obstacle Avoidance",
                "Joaquin Aguilera, Hasan Chharawalla, Odin Bruyere, Keshav Sreedharan, Cesar Garcia Perez, and Javier Gomez"
            ),
            SeniorProject(
                "CXI In-Vacuum Interaction Point: Mini Characterization Station",
                "Charmaine Lui, Tobey Chan, William Dailey, and Jeren Navarro"
            ),
            SeniorProject(
                "Portable Automatic Belay Braking System (PABBS)",
                "Alec Lefteroff, Kenneth Luu, Nathan Yoakum, and Akul Verma"
            ),
            SeniorProject(
                "Security Swarm Robots",
                "Nelson Cortez, Kylar Lee, Redge Tolentino, Michael D'amore, and Jacob Miguel Maulino"
            ),
            SeniorProject(
                "Rubik's Cube Solver",
                "Thomas Wong, Braxton Mendoza, Kevin Tran, Victor Wong, Aung Kyi Min, and Andrew Bravo"
            )
        )
    )

    private val du = SupervisorGroup(
        "Dr. Du",
        listOf(
            SeniorProject(
                "Shoulder Rehabilitation with Seven-Axis Robotic Arm",
                "Caroline Glaser, Leonardo Calle Loor, and Gerardo Saldivar"
            ),
            SeniorProject(
                "loT-Enabled Modular Housing Unit",
                "Lucca Aldana, Keely Brown, Christian Eric Balakid, Troy Buenaventura, Paul De Leon, and Vincent Vu"
            ),
            SeniorProject(
                "Solar-Powered Thermal Management and Smart System for Modular Housing",
                "Keely Brown, Nhat-Lan Nguyen, Jeanine Renoblas, Hirofumi Sato, and Dylan Tuazon"
            ),
            SeniorProject(
                "Assembly Fixtures for the Handheld Discectomy and Endplate Preparation System",
                "Landon Krivanec, Jordan Iversen, Kevin Li, Thao Nguyen, and Weston Uyekawa"
            ),
            SeniorProject(
                "Variable Neck-Pressure & Neck-Suction (NPNS) Chamber Collar",
                "Victor Baird, James Do, Derrick Fong, E T Horton, and Christopher Xiong"
            ),
            SeniorProject(
                "Air Quality Sensor Data Compensation Model and Housing Redesign",
                "Jake Holtz, Khanh Nguyen, Tayven Nguyen, Gabriel Mendoza, Hernan Mondragon-Becerra, and Will Watcha"
            )
        )
    )

    private val zaidi = SupervisorGroup(
        "Dr. Zaidi",
        listOf(
            SeniorProject(
                "3D Printing of Soft Materials/Soft Robotics using Silicone",
                "Andrey Blinkov, Philip Wollman, Rylan Wong, and Jacob Steffen-Brune"
            ),
            SeniorProject(
                "Design and Fabrication of Test Stand for Quadcopter Drone with Asymmetrical Propellers",
                "Edrick Corona Hernandez, Andrew Le, Gustav Wagner, Azeneth Muñoz, and Nolan Hujardo"
            ),
            SeniorProject(
                "Characterization of Direct-to-Chip Liquid Cooling Cold Plates",
                "Kevin Lam and Jashan Keith"
            ),
            SeniorProject(
                "Feasibility of Pre-Activating Jet Dispensed UV-Activated Adhesives",
                "Jason Sanstrom and Mason Lock"
            ),
            SeniorProject(
                "Waterway Trash-Collecting Robot",
                "Tam Bao Luong, Son Nguyen, Hoang Vu Ho, and Christopher Kintner"
            ),
            SeniorProject(
                "Solar-Powered Tiny House",
                "Ayane Gomi, Andy Luu, Luis Fernando Perez, Max Li, Krish Patel, Boutuivi Sanvee, and May Chih"
            ),
            SeniorProject(
                "UV/Ozone Plastic-to-Plastic Bonding of PMMA",
                "Antony Matei and Nasheeb Rana"
            ),
            SeniorProject(
                "Ultra Compact Scooter",
                "Roy Baek, Kyle Mizukura, Phillip Tran, and Yuki Yamamoto"
            )
        )
    )

    val allGroups: List<SupervisorGroup> = listOf(agarwal, armani, du, zaidi)
}
